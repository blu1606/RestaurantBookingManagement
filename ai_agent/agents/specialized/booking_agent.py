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
            gemini_model=gemini_model,
            service_type="BookingService"  # Chỉ xử lý BookingService tools
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
        6. Quản lý bàn: thêm, xóa, cập nhật thông tin bàn
        7. Quản lý đặt bàn: tạo, hủy, cập nhật đặt bàn
        
        Luôn đảm bảo thu thập đầy đủ thông tin cần thiết trước khi xác nhận đặt bàn.
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
        
        # 2. Fallback: Lấy context từ knowledge base (tables, bookings)
        context = self._get_relevant_context(user_input)
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về bàn và đặt bàn:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy trả lời một cách thân thiện và hữu ích. Nếu khách hàng hỏi về:
        - Thông tin bàn: Mô tả về các loại bàn, sức chứa
        - Quy trình đặt bàn: Hướng dẫn cách đặt bàn
        - Thời gian phục vụ: Thông tin về giờ mở cửa, thời gian đặt bàn
        - Chính sách đặt bàn: Quy định về hủy, thay đổi đặt bàn
        - Tư vấn bàn: Gợi ý bàn phù hợp theo số người
        
        Trả lời bằng tiếng Việt, thân thiện và hữu ích.
        """
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        response_data = self._parse_json_response(
            response_text,
            fallback_action="show_booking_info",
            fallback_response="Tôi sẽ giúp bạn với thông tin đặt bàn. Bạn cần hỗ trợ gì?"
        )
        return self.create_response(
            action=response_data.get("action", "show_booking_info"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Tôi sẽ giúp bạn với thông tin đặt bàn.")
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