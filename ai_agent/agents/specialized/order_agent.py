import json
from typing import Dict, Any
from ..core.base_agent import BaseAgent

class OrderAgent(BaseAgent):
    """
    AI Agent chuyên xử lý đơn hàng và thanh toán
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="OrderAgent",
            data_files=["orders.json", "menu_items.json", "bookings.json"],
            gemini_model=gemini_model,
            service_type="OrderService"  # Chỉ xử lý OrderService tools
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là nhân viên phục vụ chuyên nghiệp của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Xử lý đơn hàng: thêm món, xóa món, hoàn thành đơn hàng
        2. Tính tiền và thanh toán cho khách hàng
        3. Quản lý trạng thái đơn hàng
        4. Báo cáo doanh thu và thống kê
        5. Hỗ trợ khách hàng với các vấn đề về đơn hàng
        
        Luôn đảm bảo tính chính xác khi xử lý đơn hàng và thanh toán.
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
        
        # 2. Fallback: Lấy context từ knowledge base (orders, menu)
        context = self._get_relevant_context(user_input)
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về đơn hàng và menu:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy trả lời một cách thân thiện và hữu ích.
        """
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        response_data = self._parse_json_response(
            response_text,
            fallback_action="show_order_info",
            fallback_response="Tôi sẽ giúp bạn với đơn hàng. Bạn cần hỗ trợ gì?"
        )
        return self.create_response(
            action=response_data.get("action", "show_order_info"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Tôi sẽ giúp bạn với đơn hàng.")
        )
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format order knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "order":
                return f"Đơn hàng {item.get('orderId', '')}: {item.get('status', '')} - {item.get('totalAmount', '')} VND"
            elif item.get("type") == "menu_item":
                return f"Món: {item.get('name', '')} - Giá: {item.get('price', '')} VND"
            elif item.get("type") == "payment":
                return f"Thanh toán: {item.get('amount', '')} VND - {item.get('method', '')}"
        return super()._format_knowledge_item(item) 