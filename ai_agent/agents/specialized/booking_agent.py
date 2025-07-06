import json
from typing import Dict, Any
from ..core.base_agent import BaseAgent

class BookingAgent(BaseAgent):
    """
    AI Agent chuyên xử lý đặt bàn và kiểm tra lịch trống
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="BookingAgent",
            data_files=["tables.json", "bookings.json", "customers.json"],
            gemini_model=gemini_model
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là nhân viên lễ tân chuyên nghiệp của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Xử lý yêu cầu đặt bàn của khách hàng
        2. Kiểm tra tính khả dụng của bàn theo thời gian và số người
        3. Thu thập thông tin cần thiết: tên, số điện thoại, số người, thời gian
        4. Xác nhận thông tin đặt bàn
        5. Hướng dẫn quy trình đặt bàn
        
        Luôn đảm bảo thu thập đầy đủ thông tin cần thiết trước khi xác nhận đặt bàn.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # Lấy context từ knowledge base (booking info, table status)
        context = self._get_relevant_context(user_input)
        
        # Tạo prompt cho Gemini
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về bàn và đặt bàn:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy phân tích yêu cầu và trả về JSON với format phù hợp:
        
        Nếu khách hàng muốn đặt bàn và đã có đủ thông tin:
        {{
            "action": "book_table",
            "parameters": {{
                "customerName": "tên khách hàng",
                "customerPhone": "số điện thoại",
                "guests": "số người",
                "dateTime": "ngày giờ đặt bàn",
                "specialRequests": "yêu cầu đặc biệt nếu có"
            }},
            "naturalResponse": "Câu trả lời xác nhận đặt bàn"
        }}
        
        Nếu khách hàng muốn đặt bàn nhưng thiếu thông tin:
        {{
            "action": "collect_booking_info",
            "parameters": {{
                "missingInfo": "thông tin còn thiếu",
                "currentInfo": "thông tin đã có"
            }},
            "naturalResponse": "Câu trả lời yêu cầu thông tin bổ sung"
        }}
        
        Nếu khách hàng kiểm tra bàn trống:
        {{
            "action": "check_availability",
            "parameters": {{
                "date": "ngày kiểm tra",
                "time": "giờ kiểm tra",
                "guests": "số người"
            }},
            "naturalResponse": "Câu trả lời về tình trạng bàn"
        }}
        
        Nếu khách hàng hỏi về quy trình đặt bàn:
        {{
            "action": "booking_info",
            "parameters": {{}},
            "naturalResponse": "Câu trả lời hướng dẫn đặt bàn"
        }}
        """
        
        # Gọi Gemini với chat_session
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        
        # Parse JSON response with improved error handling
        response_data = self._parse_json_response(
            response_text, 
            fallback_action="collect_booking_info",
            fallback_response="Tôi sẽ giúp bạn đặt bàn. Bạn có thể cho tôi biết tên và số điện thoại không?"
        )
        
        return self.create_response(
            action=response_data.get("action", "collect_booking_info"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Tôi sẽ giúp bạn đặt bàn. Bạn có thể cho tôi biết tên và số điện thoại không?")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format booking knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "table":
                return f"Bàn {item.get('tableId', '')}: {item.get('capacity', '')} người - Trạng thái: {item.get('status', '')}"
            elif item.get("type") == "booking":
                return f"Đặt bàn {item.get('bookingId', '')}: {item.get('customerName', '')} - {item.get('guests', '')} người - {item.get('dateTime', '')}"
            elif item.get("type") == "policy":
                return f"Chính sách: {item.get('title', '')} - {item.get('description', '')}"
        return super()._format_knowledge_item(item) 