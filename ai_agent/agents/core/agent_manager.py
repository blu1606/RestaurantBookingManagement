import os
import json
from typing import Dict, Any, Optional
from .router_ai import RouterAI
from ..specialized.greeting_agent import GreetingAgent
from ..specialized.menu_agent import MenuAgent
from ..specialized.booking_agent import BookingAgent
from ..specialized.cancellation_agent import CancellationAgent
from ..specialized.information_agent import InformationAgent
from ..specialized.feedback_agent import FeedbackAgent
from ..specialized.fallback_agent import FallbackAgent
from ..specialized.order_agent import OrderAgent
import threading

class AgentManager:
    """
    Manager để quản lý và điều phối tất cả các AI Agent
    """
    
    def __init__(self, gemini_model=None):
        self.gemini_model = gemini_model
        self.router = RouterAI()
        self.agents = {}
        self.conversation_history = {}
        self.chat_sessions = {}  # Lưu chat sessions cho từng người dùng
        self.pending_actions = {}  # NEW: Lưu trạng thái pending action cho từng session
        
        # Khởi tạo các agent
        self._initialize_agents()
    
    def _initialize_agents(self):
        """
        Khởi tạo tất cả các AI Agent song song bằng threading
        """
        try:
            agent_classes = [
                ("GreetingAgent", GreetingAgent, {}),
                ("MenuAgent", MenuAgent, {"user_role": "user"}),
                ("BookingAgent", BookingAgent, {"user_role": "user"}),
                ("CancellationAgent", CancellationAgent, {"user_role": "user"}),
                ("InformationAgent", InformationAgent, {}),
                ("FeedbackAgent", FeedbackAgent, {}),
                ("OrderAgent", OrderAgent, {"user_role": "user"}),
                ("FallbackAgent", FallbackAgent, {})
            ]
            agents = {}
            threads = []
            def create_agent(name, cls, kwargs):
                agents[name] = cls(self.gemini_model, **kwargs) if kwargs else cls(self.gemini_model)
            for name, cls, kwargs in agent_classes:
                t = threading.Thread(target=create_agent, args=(name, cls, kwargs))
                threads.append(t)
                t.start()
            for t in threads:
                t.join()
            self.agents = agents
            print("✅ All AI Agents initialized successfully (threaded)")
        except Exception as e:
            print(f"🔥 Error initializing agents: {e}")
    
    def process_user_input(self, user_input: str, session_id: str = "default", role: str = "user", user_info: dict = None) -> Dict[str, Any]:
        """
        Xử lý input của user thông qua Router AI và các Agent chuyên biệt
        """
        try:
            # Lấy hoặc tạo ChatSession cho session_id
            if session_id not in self.chat_sessions:
                if self.gemini_model:
                    self.chat_sessions[session_id] = self.gemini_model.start_chat(history=[])
                else:
                    self.chat_sessions[session_id] = None
            
            current_chat_session = self.chat_sessions[session_id]

            # 0. Nếu session này đang có pending action (ask_for_info), tiếp tục với tool/action đó
            if session_id in self.pending_actions:
                pending = self.pending_actions[session_id]
                agent_name = pending["agent_name"]
                tool = pending["tool"]
                collected_params = pending["collected_params"]
                missing_params = pending["missing_params"]
                # Lấy agent tương ứng
                agent = self.agents.get(agent_name)
                if not agent:
                    agent = self.agents.get("FallbackAgent")
                # Gọi agent để bổ sung thông tin
                # Agent cần có hàm bổ sung params (nếu chưa có thì sẽ bổ sung ở agent)
                response = agent.handle_pending_action(user_input, tool, collected_params, missing_params, session_id, chat_session=current_chat_session)
                # Nếu đã đủ params, xóa pending
                if response.get("action") != "ask_for_info":
                    del self.pending_actions[session_id]
                else:
                    # Nếu vẫn thiếu, cập nhật lại collected_params, missing_params
                    self.pending_actions[session_id]["collected_params"] = response.get("collected_params", collected_params)
                    self.pending_actions[session_id]["missing_params"] = response.get("missing_params", missing_params)
                # Bổ sung routing info cho response
                response["routing"] = {
                    "intent": pending.get("intent", "pending"),
                    "agent": agent_name,
                    "confidence": 1.0
                }
                self._update_conversation_history(session_id, user_input, response)
                return response

            # 1. Router AI phân tích ý định
            routing_result = self.router.route_to_agent(user_input)
            agent_name = routing_result["agent"]
            intent = routing_result["intent"]
            confidence = routing_result["confidence"]
            
            # Convert confidence to float if it's a string
            confidence_float = float(confidence) if isinstance(confidence, str) else confidence
            print(f"🎯 Router AI detected intent: {intent} -> {agent_name} (confidence: {confidence_float:.2f})")
            
            # 2. Lấy agent tương ứng
            agent = self.agents.get(agent_name)
            if not agent:
                print(f"⚠️ Agent {agent_name} not found, using FallbackAgent")
                agent = self.agents.get("FallbackAgent")
            
            # 3. Cập nhật role cho agent nếu cần
            if hasattr(agent, 'user_role') and role != agent.user_role:
                # Tạo agent mới với role mới
                try:
                    agent.user_role = role
                    # Nếu cần, reload permissions/tools cho role mới
                    if hasattr(agent, '_load_permissions'):
                        agent._load_permissions()
                    if hasattr(agent, '_load_tools'):
                        agent._load_tools()
                    print(f"🔄 Updated {agent_name} with role '{role}' (no re-init)")
                except Exception as e:
                    print(f"⚠️ Failed to update agent role: {e}")
            
            # 4. Xử lý yêu cầu bằng agent chuyên biệt, truyền chat_session
            if agent_name == "BookingAgent":
                response = agent.process_request(user_input, session_id, chat_session=current_chat_session, user_info=user_info)
            else:
                response = agent.process_request(user_input, session_id, chat_session=current_chat_session)
            
            # Nếu response là ask_for_info, lưu pending action
            if response.get("action") == "ask_for_info":
                self.pending_actions[session_id] = {
                    "agent_name": agent_name,
                    "tool": response.get("original_tool", {}),
                    "collected_params": response.get("collected_params", {}),
                    "missing_params": response.get("parameters", {}).get("missing_params", []),
                    "intent": intent
                }
            
            # 5. Thêm thông tin routing vào response
            response["routing"] = {
                "intent": intent,
                "agent": agent_name,
                "confidence": confidence_float
            }
            
            # 6. Cập nhật conversation history
            self._update_conversation_history(session_id, user_input, response)
            
            return response
            
        except Exception as e:
            print(f"🔥 Error in AgentManager: {e}")
            # Fallback response
            return {
                "action": "error",
                "parameters": {},
                "naturalResponse": "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.",
                "agent": "FallbackAgent",
                "routing": {
                    "intent": "fallback",
                    "agent": "FallbackAgent",
                    "confidence": 0.00
                }
            }
    
    def _update_conversation_history(self, session_id: str, user_input: str, response: Dict[str, Any]):
        """
        Cập nhật lịch sử hội thoại
        """
        if session_id not in self.conversation_history:
            self.conversation_history[session_id] = []
        
        # Thêm user input
        self.conversation_history[session_id].append(f"User: {user_input}")
        
        # Thêm AI response
        if "naturalResponse" in response:
            self.conversation_history[session_id].append(f"AI: {response['naturalResponse']}")
        
        # Giữ chỉ 10 tin nhắn gần nhất
        if len(self.conversation_history[session_id]) > 10:
            self.conversation_history[session_id] = self.conversation_history[session_id][-10:]
    
    def refresh_all_knowledge(self):
        """
        Refresh knowledge base cho tất cả agents
        """
        try:
            for agent_name, agent in self.agents.items():
                agent.refresh_knowledge()
            print("✅ All agent knowledge bases refreshed")
        except Exception as e:
            print(f"🔥 Error refreshing knowledge bases: {e}")
    
    def get_agent_status(self) -> Dict[str, Any]:
        """
        Lấy trạng thái của tất cả agents
        """
        status = {}
        for agent_name, agent in self.agents.items():
            status[agent_name] = {
                "knowledge_loaded": len(agent.knowledge_base) > 0,
                "vector_db_ready": agent.retriever is not None,
                "knowledge_count": len(agent.knowledge_base)
            }
        return status
    
    def get_conversation_history(self, session_id: str = "default") -> list:
        """
        Lấy lịch sử hội thoại cho session
        """
        return self.conversation_history.get(session_id, [])
    
    def clear_conversation_history(self, session_id: str = "default"):
        """
        Xóa lịch sử hội thoại cho session
        """
        if session_id in self.conversation_history:
            del self.conversation_history[session_id]
            print(f"🗑️ Conversation history cleared for session {session_id}")

# Test function
def test_agent_manager():
    """
    Test Agent Manager với các trường hợp khác nhau
    """
    manager = AgentManager()
    
    test_cases = [
        "Chào bạn",
        "Cho tôi xem menu",
        "Tôi muốn đặt bàn cho 4 người",
        "Nhà hàng mở cửa đến mấy giờ?",
        "Món ăn rất ngon!",
        "Thời tiết hôm nay thế nào?"
    ]
    
    print("🧪 Testing Agent Manager...")
    print("=" * 50)
    
    for i, user_input in enumerate(test_cases, 1):
        print(f"\nTest {i}: {user_input}")
        response = manager.process_user_input(user_input)
        print(f"Intent: {response['routing']['intent']}")
        print(f"Agent: {response['routing']['agent']}")
        print(f"Confidence: {response['routing']['confidence']:.2f}")
        print(f"Response: {response['naturalResponse']}")
        print("-" * 30)

if __name__ == "__main__":
    test_agent_manager() 