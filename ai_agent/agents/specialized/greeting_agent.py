import json
from typing import Dict, Any

from ..core.base_agent import BaseAgent


class GreetingAgent(BaseAgent):
    """
    AI Agent chuyên xử lý lời chào hỏi và giới thiệu
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="GreetingAgent",
            data_files=["knowledge/greeting_knowledge.json"],
            gemini_model=gemini_model
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là một trợ lý AI thân thiện của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Chào hỏi khách hàng một cách lịch sự và thân thiện
        2. Giới thiệu về nhà hàng và các dịch vụ có sẵn
        3. Hướng dẫn khách hàng về cách sử dụng hệ thống
        4. Trả lời các câu hỏi chung về nhà hàng
        
        Luôn sử dụng ngôn ngữ lịch sự, thân thiện và phù hợp với văn hóa Việt Nam.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # Lấy context từ knowledge base
        context = self._get_relevant_context(user_input)
        
        # Tạo prompt cho Gemini
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về nhà hàng:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy trả lời một cách thân thiện và hữu ích. Trả về JSON với format:
        {{
            "action": "greeting",
            "parameters": {{}},
            "naturalResponse": "Câu trả lời của bạn"
        }}
        """
        
        # Gọi Gemini với chat_session
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Parse JSON response with improved error handling
        response_data = self._parse_json_response(
            response_text,
            fallback_action="greeting",
            fallback_response="Xin chào! Tôi là trợ lý AI của nhà hàng. Tôi có thể giúp bạn đặt bàn, xem menu, hoặc cung cấp thông tin về nhà hàng. Bạn cần gì ạ?"
        )
        
        return self.create_response(
            action=response_data.get("action", "greeting"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Xin chào! Tôi có thể giúp gì cho bạn?")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format greeting knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "greeting":
                return f"Lời chào: {item.get('content', '')}"
            elif item.get("type") == "introduction":
                return f"Giới thiệu: {item.get('content', '')}"
            elif item.get("type") == "service":
                return f"Dịch vụ: {item.get('name', '')} - {item.get('description', '')}"
        return super()._format_knowledge_item(item) 