import json
from typing import Dict, Any
from ..core.base_agent import BaseAgent
from ..utils import handle_pending_action_utils

class BookingAgent(BaseAgent):
    """
    AI Agent chuyên xử lý đặt bàn và kiểm tra lịch trống
    """
    
    def __init__(self, gemini_model=None, user_role: str = "user"):
        super().__init__(
            agent_name="BookingAgent",
            data_files=["tables.json", "bookings.json", "customers.json"],
            gemini_model=gemini_model,
            user_role=user_role
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
        
        QUAN TRỌNG: 
        - Luôn thu thập đầy đủ thông tin cần thiết trước khi xác nhận đặt bàn
        - Nếu thiếu thông tin, hãy hỏi thêm một cách thân thiện
        - Thông tin cần thiết cho đặt bàn: tên khách hàng, số điện thoại, số người, thời gian
        - Luôn xác nhận thông tin trước khi thực hiện đặt bàn
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None, user_info=None) -> Dict[str, Any]:
        tool = self.detect_tool_from_prompt(user_input)
        if tool:
            extracted_params = self._extract_booking_parameters(user_input, tool)
            # Nếu có user_info, bổ sung vào params nếu thiếu
            if user_info:
                for k in ["customerName", "customerPhone", "customerEmail"]:
                    if k in tool.get("parameters", []) and (k not in extracted_params or not extracted_params[k]):
                        val = user_info.get(k)
                        if val:
                            extracted_params[k] = val
            missing_params = self._check_missing_parameters(extracted_params, tool)
            if missing_params:
                return self._ask_for_missing_info(missing_params, tool, user_input)
            else:
                return self.create_response(
                    action=tool["name"],
                    parameters=extracted_params,
                    natural_response=f"Tôi sẽ thực hiện tác vụ: {tool['description']} với thông tin đã cung cấp."
                )
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
    
    def _extract_booking_parameters(self, user_input: str, tool: Dict[str, Any]) -> Dict[str, Any]:
        """
        Trích xuất thông tin từ user input
        """
        import re
        params = {}
        user_input_lower = user_input.lower()
        
        # Trích xuất thông tin cơ bản
        if "create_booking" in tool.get("name", ""):
            # Tên khách hàng
            name_match = re.search(r'cho\s+(\w+)', user_input_lower)
            if name_match:
                params["customerName"] = name_match.group(1).title()
            
            # Số điện thoại
            phone_match = re.search(r'(\d{10,11})', user_input)
            if phone_match:
                params["customerPhone"] = phone_match.group(1)
            
            # Số người
            guests_match = re.search(r'(\d+)\s*người', user_input_lower)
            if guests_match:
                params["guests"] = int(guests_match.group(1))
            
            # Thời gian
            time_match = re.search(r'vào\s+(\d+h?\s*\d*)\s*(tối|sáng|chiều)?', user_input_lower)
            if time_match:
                time_str = time_match.group(1)
                period = time_match.group(2) or ""
                params["dateTime"] = self._format_time(time_str, period)
        
        elif "update_booking" in tool.get("name", ""):
            # Booking ID
            id_match = re.search(r'(\d+)', user_input)
            if id_match:
                params["bookingId"] = int(id_match.group(1))
            
            # Số người
            guests_match = re.search(r'(\d+)\s*người', user_input_lower)
            if guests_match:
                params["guests"] = int(guests_match.group(1))
        
        elif "cancel_booking" in tool.get("name", ""):
            # Booking ID
            id_match = re.search(r'(\d+)', user_input)
            if id_match:
                params["bookingId"] = int(id_match.group(1))
        
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
        Hỏi thêm thông tin thiếu
        """
        tool_name = tool.get("name", "")
        
        if "create_booking" in tool_name:
            questions = []
            if "customerName" in missing_params:
                questions.append("Tên của bạn là gì?")
            if "customerPhone" in missing_params:
                questions.append("Số điện thoại của bạn là gì?")
            if "guests" in missing_params:
                questions.append("Bạn muốn đặt bàn cho mấy người?")
            if "dateTime" in missing_params:
                questions.append("Bạn muốn đặt bàn vào thời gian nào? (VD: 19h tối nay, 12h trưa mai)")
            
            response = f"Để đặt bàn, tôi cần thêm một số thông tin:\n"
            response += "\n".join([f"- {q}" for q in questions])
            response += "\n\nBạn có thể cung cấp thông tin này không?"
        
        elif "update_booking" in tool_name:
            if "bookingId" in missing_params:
                response = "Bạn muốn cập nhật đặt bàn nào? Vui lòng cho biết mã đặt bàn."
            else:
                response = "Bạn muốn cập nhật thông tin gì? (số người, thời gian)"
        
        elif "cancel_booking" in tool_name:
            if "bookingId" in missing_params:
                response = "Bạn muốn hủy đặt bàn nào? Vui lòng cho biết mã đặt bàn."
            else:
                response = "Tôi cần mã đặt bàn để hủy cho bạn."
        
        else:
            response = "Tôi cần thêm thông tin để thực hiện yêu cầu của bạn."
        
        return self.create_response(
            action="ask_for_info",
            parameters={"missing_params": missing_params, "original_tool": tool_name},
            natural_response=response
        )
    
    def _format_time(self, time_str: str, period: str) -> str:
        """
        Format thời gian từ user input
        """
        # Đơn giản hóa - có thể cải thiện thêm
        if "tối" in period:
            return f"{time_str} tối nay"
        elif "sáng" in period:
            return f"{time_str} sáng mai"
        elif "chiều" in period:
            return f"{time_str} chiều nay"
        else:
            return f"{time_str} tối nay"  # Default 

    def handle_pending_action(self, user_input, tool, collected_params, missing_params, session_id, chat_session=None):
        return handle_pending_action_utils(
            user_input,
            tool,
            collected_params,
            missing_params,
            self._extract_booking_parameters,
            self._check_missing_parameters,
            self._ask_for_missing_info,
            self.create_response
        ) 