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
        
        # Khởi tạo các agent
        self._initialize_agents()
    
    def _initialize_agents(self):
        """
        Khởi tạo tất cả các AI Agent
        """
        try:
            self.agents = {
                "GreetingAgent": GreetingAgent(self.gemini_model),
                "MenuAgent": MenuAgent(self.gemini_model),
                "BookingAgent": BookingAgent(self.gemini_model),
                "CancellationAgent": CancellationAgent(self.gemini_model),
                "InformationAgent": InformationAgent(self.gemini_model),
                "FeedbackAgent": FeedbackAgent(self.gemini_model),
                "OrderAgent": OrderAgent(self.gemini_model),
                "FallbackAgent": FallbackAgent(self.gemini_model)
            }
            print("✅ All AI Agents initialized successfully")
        except Exception as e:
            print(f"🔥 Error initializing agents: {e}")
    
    def process_user_input(self, user_input: str, session_id: str = "default", role: str = "USER") -> Dict[str, Any]:
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
            
            # 3. Xử lý yêu cầu bằng agent chuyên biệt, truyền chat_session
            response = agent.process_request(user_input, session_id, chat_session=current_chat_session)
            
            # 4. Thêm thông tin routing vào response
            response["routing"] = {
                "intent": intent,
                "agent": agent_name,
                "confidence": confidence_float
            }
            
            # 5. Cập nhật conversation history
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