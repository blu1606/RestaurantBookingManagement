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
        # Lấy context từ knowledge base
        context = self._get_relevant_context(user_input)
        
        # Tạo prompt cho Gemini
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về chính sách hủy và thay đổi đặt bàn:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy phân tích yêu cầu và trả về JSON với format phù hợp:
        
        Nếu khách hàng muốn hủy đặt bàn:
        {{
            "action": "cancel_booking",
            "parameters": {{
                "bookingId": "mã đặt bàn",
                "reason": "lý do hủy nếu có"
            }},
            "naturalResponse": "Câu trả lời xác nhận hủy đặt bàn"
        }}
        
        Nếu khách hàng muốn thay đổi đặt bàn:
        {{
            "action": "modify_booking",
            "parameters": {{
                "bookingId": "mã đặt bàn",
                "newDateTime": "thời gian mới",
                "newGuests": "số người mới",
                "changes": "thay đổi cụ thể"
            }},
            "naturalResponse": "Câu trả lời xác nhận thay đổi đặt bàn"
        }}
        
        Nếu khách hàng cần thông tin về chính sách:
        {{
            "action": "booking_policy",
            "parameters": {{
                "policyType": "loại chính sách"
            }},
            "naturalResponse": "Câu trả lời về chính sách"
        }}
        
        Nếu khách hàng cần tìm đặt bàn:
        {{
            "action": "find_booking",
            "parameters": {{
                "searchBy": "tìm theo tên/số điện thoại/mã đặt bàn"
            }},
            "naturalResponse": "Câu trả lời hướng dẫn tìm đặt bàn"
        }}
        """
        
        # Gọi Gemini với chat_session
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Parse JSON response with improved error handling
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