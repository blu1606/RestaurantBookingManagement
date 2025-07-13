import json
from typing import Dict, Any
from ..core.base_agent import BaseAgent
from ..utils import handle_pending_action_utils


class CancellationAgent(BaseAgent):
    """
    AI Agent chuyên xử lý hủy và thay đổi đặt bàn
    """
    
    def __init__(self, gemini_model=None, user_role: str = "user"):
        super().__init__(
            agent_name="CancellationAgent",
            data_files=["bookings.json", "customers.json"],
            gemini_model=gemini_model,
            user_role=user_role
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
        
        QUAN TRỌNG: 
        - Luôn thu thập đầy đủ thông tin cần thiết trước khi thực hiện hủy/thay đổi
        - Nếu thiếu thông tin, hãy hỏi thêm một cách lịch sự và kiên nhẫn
        - Thông tin cần thiết: mã đặt bàn, thông tin thay đổi (số người, thời gian)
        - Luôn xác nhận thông tin trước khi thực hiện thao tác
        - Luôn lịch sự và kiên nhẫn khi xử lý yêu cầu hủy/thay đổi đặt bàn
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # Ưu tiên dùng Gemini để suggest tool
        tool_name = None
        if self.tool_detector:
            tool_name = self.tool_detector.suggest_tool_with_gemini(user_input)
        tool = None
        if tool_name:
            tool = self.tool_detector.get_tool_by_name(tool_name)
        # Nếu không có tool, KHÔNG fallback detect_tool_from_prompt, chuyển sang trả lời tự nhiên
        if tool:
            # Extract parameters từ user input
            extracted_params = self._extract_cancellation_parameters(user_input, tool)
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
        # Nếu không có tool, trả lời tự nhiên dựa vào context/knowledge base
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
    
    def _extract_cancellation_parameters(self, user_input: str, tool: Dict[str, Any]) -> Dict[str, Any]:
        """
        Trích xuất thông tin từ user input cho cancellation
        """
        import re
        params = {}
        user_input_lower = user_input.lower()
        
        # Trích xuất thông tin cơ bản
        if "cancel_booking" in tool.get("name", ""):
            # Booking ID
            booking_match = re.search(r'đặt bàn\s+(\d+)', user_input_lower)
            if not booking_match:
                booking_match = re.search(r'booking\s+(\d+)', user_input_lower)
            if not booking_match:
                booking_match = re.search(r'(\d+)', user_input_lower)
            if booking_match:
                params["bookingId"] = int(booking_match.group(1))
        
        elif "update_booking" in tool.get("name", ""):
            # Booking ID
            booking_match = re.search(r'đặt bàn\s+(\d+)', user_input_lower)
            if not booking_match:
                booking_match = re.search(r'booking\s+(\d+)', user_input_lower)
            if not booking_match:
                booking_match = re.search(r'(\d+)', user_input_lower)
            if booking_match:
                params["bookingId"] = int(booking_match.group(1))
            
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
        Hỏi thêm thông tin thiếu cho cancellation
        """
        tool_name = tool.get("name", "")
        
        if "cancel_booking" in tool_name:
            if "bookingId" in missing_params:
                response = "Bạn muốn hủy đặt bàn nào? Vui lòng cho biết mã đặt bàn (VD: đặt bàn 10)."
            else:
                response = "Tôi cần mã đặt bàn để hủy cho bạn."
        
        elif "update_booking" in tool_name:
            questions = []
            if "bookingId" in missing_params:
                questions.append("Bạn muốn cập nhật đặt bàn nào? Vui lòng cho biết mã đặt bàn.")
            if "guests" in missing_params and "dateTime" in missing_params:
                questions.append("Bạn muốn thay đổi thông tin gì? (số người, thời gian)")
            elif "guests" in missing_params:
                questions.append("Bạn muốn thay đổi số người thành bao nhiêu?")
            elif "dateTime" in missing_params:
                questions.append("Bạn muốn thay đổi thời gian thành khi nào?")
            
            if questions:
                response = f"Để cập nhật đặt bàn, tôi cần thêm thông tin:\n"
                response += "\n".join([f"- {q}" for q in questions])
                response += "\n\nBạn có thể cung cấp thông tin này không?"
            else:
                response = "Tôi cần thêm thông tin để cập nhật đặt bàn cho bạn."
        
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
            self._extract_cancellation_parameters,
            self._check_missing_parameters,
            self._ask_for_missing_info,
            self.create_response
        ) 