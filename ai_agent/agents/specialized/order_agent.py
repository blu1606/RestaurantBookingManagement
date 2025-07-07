import json
from typing import Dict, Any
from ..core.base_agent import BaseAgent
from ..utils import handle_pending_action_utils

class OrderAgent(BaseAgent):
    """
    AI Agent chuyên xử lý đơn hàng và thanh toán
    """
    
    def __init__(self, gemini_model=None, user_role: str = "user"):
        super().__init__(
            agent_name="OrderAgent",
            data_files=["orders.json", "menu_items.json", "bookings.json"],
            gemini_model=gemini_model,
            user_role=user_role
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
        
        QUAN TRỌNG: 
        - Luôn thu thập đầy đủ thông tin cần thiết trước khi xử lý đơn hàng
        - Nếu thiếu thông tin, hãy hỏi thêm một cách thân thiện
        - Thông tin cần thiết: mã đơn hàng, tên món, số lượng
        - Luôn xác nhận thông tin trước khi thực hiện thao tác
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # 1. Check if user_input matches any tool
        tool = self.detect_tool_from_prompt(user_input)
        if tool:
            # Extract parameters từ user input
            extracted_params = self._extract_order_parameters(user_input, tool)
            
            # Kiểm tra xem có đủ thông tin không
            missing_params = self._check_missing_parameters(extracted_params, tool)
            
            if missing_params:
                # Nếu thiếu thông tin, hỏi thêm
                return self._ask_for_missing_info(missing_params, tool, user_input)
            else:
                # Nếu đủ thông tin, thực hiện action
                return self.create_response(
                    action=tool["name"],
                    parameters=extracted_params,
                    natural_response=f"Tôi sẽ thực hiện tác vụ: {tool['description']} với thông tin đã cung cấp."
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
    
    def _extract_order_parameters(self, user_input: str, tool: Dict[str, Any]) -> Dict[str, Any]:
        """
        Trích xuất thông tin từ user input cho order
        """
        import re
        params = {}
        user_input_lower = user_input.lower()
        
        # Trích xuất thông tin cơ bản
        if "add_item_to_order" in tool.get("name", ""):
            # Order ID
            order_match = re.search(r'đơn hàng\s+(\d+)', user_input_lower)
            if order_match:
                params["orderId"] = int(order_match.group(1))
            
            # Tên món
            item_match = re.search(r'(?:thêm|đặt)\s+(\d+)?\s*(\w+(?:\s+\w+)*?)(?:\s+(\d+))?', user_input_lower)
            if item_match:
                quantity = item_match.group(1) or item_match.group(3) or "1"
                item_name = item_match.group(2)
                params["itemName"] = item_name.strip()
                params["quantity"] = int(quantity)
        
        elif "remove_item_from_order" in tool.get("name", ""):
            # Order ID
            order_match = re.search(r'đơn hàng\s+(\d+)', user_input_lower)
            if order_match:
                params["orderId"] = int(order_match.group(1))
            
            # Tên món
            item_match = re.search(r'(?:xóa|bỏ)\s+(\w+(?:\s+\w+)*?)', user_input_lower)
            if item_match:
                params["itemName"] = item_match.group(1).strip()
        
        elif "complete_order" in tool.get("name", ""):
            # Order ID
            order_match = re.search(r'đơn hàng\s+(\d+)', user_input_lower)
            if order_match:
                params["orderId"] = int(order_match.group(1))
        
        elif "calculate_bill" in tool.get("name", ""):
            # Booking ID
            booking_match = re.search(r'booking\s+(\d+)', user_input_lower)
            if booking_match:
                params["bookingId"] = int(booking_match.group(1))
        
        return params
    
    def _check_missing_parameters(self, params: Dict[str, Any], tool: Dict[str, Any]) -> list:
        """
        Kiểm tra thông tin thiếu
        """
        required_params = tool.get("parameters", [])
        missing = []
        
        for param in required_params:
            if param not in params or params[param] is None:
                missing.append(param)
        
        return missing
    
    def _ask_for_missing_info(self, missing_params: list, tool: Dict[str, Any], original_input: str) -> Dict[str, Any]:
        """
        Hỏi thêm thông tin thiếu cho order
        """
        tool_name = tool.get("name", "")
        
        if "add_item_to_order" in tool_name:
            questions = []
            if "orderId" in missing_params:
                questions.append("Bạn muốn thêm món vào đơn hàng nào? (VD: đơn hàng 5)")
            if "itemName" in missing_params:
                questions.append("Bạn muốn thêm món gì?")
            if "quantity" in missing_params:
                questions.append("Bạn muốn thêm bao nhiêu phần?")
            
            response = f"Để thêm món vào đơn hàng, tôi cần thêm thông tin:\n"
            response += "\n".join([f"- {q}" for q in questions])
            response += "\n\nBạn có thể cung cấp thông tin này không?"
        
        elif "remove_item_from_order" in tool_name:
            questions = []
            if "orderId" in missing_params:
                questions.append("Bạn muốn xóa món khỏi đơn hàng nào? (VD: đơn hàng 5)")
            if "itemName" in missing_params:
                questions.append("Bạn muốn xóa món gì?")
            
            response = f"Để xóa món khỏi đơn hàng, tôi cần thêm thông tin:\n"
            response += "\n".join([f"- {q}" for q in questions])
            response += "\n\nBạn có thể cung cấp thông tin này không?"
        
        elif "complete_order" in tool_name:
            if "orderId" in missing_params:
                response = "Bạn muốn hoàn thành đơn hàng nào? Vui lòng cho biết mã đơn hàng."
            else:
                response = "Tôi cần mã đơn hàng để hoàn thành cho bạn."
        
        elif "calculate_bill" in tool_name:
            if "bookingId" in missing_params:
                response = "Bạn muốn tính tiền cho booking nào? Vui lòng cho biết mã booking."
            else:
                response = "Tôi cần mã booking để tính tiền cho bạn."
        
        else:
            response = "Tôi cần thêm thông tin để thực hiện yêu cầu của bạn."
        
        return self.create_response(
            action="ask_for_info",
            parameters={"missing_params": missing_params, "original_tool": tool_name},
            natural_response=response
        )
    
    def handle_pending_action(self, user_input, tool, collected_params, missing_params, session_id, chat_session=None):
        return handle_pending_action_utils(
            user_input,
            tool,
            collected_params,
            missing_params,
            self._extract_order_parameters,
            self._check_missing_parameters,
            self._ask_for_missing_info,
            self.create_response
        ) 