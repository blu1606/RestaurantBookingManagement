import json
from typing import Dict, Any
from ..core.base_agent import BaseAgent


class CancellationAgent(BaseAgent):
    """
    AI Agent chuyên xử lý hủy và thay đổi đặt bàn
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="CancellationAgent",
            data_files=["bookings.json", "customers.json"],
            gemini_model=gemini_model
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là nhân viên hỗ trợ khách hàng chuyên nghiệp của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Hỗ trợ khách hàng hủy đặt bàn
        2. Hỗ trợ thay đổi thông tin đặt bàn (thời gian, số người, v.v.)
        3. Giải thích chính sách hủy và thay đổi đặt bàn
        4. Xác nhận thông tin trước khi thực hiện thay đổi
        5. Hướng dẫn quy trình hủy/thay đổi đặt bàn
        
        Luôn lịch sự và kiên nhẫn khi xử lý yêu cầu hủy/thay đổi đặt bàn.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # 1. Check if user_input matches any tool
        tool = self.detect_tool_from_prompt(user_input)
        if tool:
            # Extract parameters (simple: just return empty or all None, real use: NLP extract)
            parameters = {param: None for param in tool.get("parameters", [])}
            return self.create_response(
                action=tool["name"],
                parameters=parameters,
                natural_response=f"Tôi sẽ thực hiện tác vụ: {tool['description']} (service: {tool['service']})"
            )
        # 2. Fallback: Lấy context từ knowledge base
        context = self._get_relevant_context(user_input)
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về chính sách hủy và thay đổi đặt bàn:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy trả lời một cách thân thiện và hữu ích.
        """
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        response_data = self._parse_json_response(
            response_text,
            fallback_action="booking_policy",
            fallback_response="Tôi sẽ giúp bạn hủy hoặc thay đổi đặt bàn. Bạn có thể cho tôi biết mã đặt bàn không?"
        )
        return self.create_response(
            action=response_data.get("action", "booking_policy"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Tôi sẽ giúp bạn hủy hoặc thay đổi đặt bàn. Bạn có thể cho tôi biết mã đặt bàn không?")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format cancellation knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "policy":
                return f"Chính sách: {item.get('title', '')} - {item.get('description', '')}"
            elif item.get("type") == "booking":
                return f"Đặt bàn {item.get('bookingId', '')}: {item.get('customerName', '')} - {item.get('dateTime', '')}"
            elif item.get("type") == "procedure":
                return f"Quy trình: {item.get('title', '')} - {item.get('steps', '')}"
        return super()._format_knowledge_item(item) 