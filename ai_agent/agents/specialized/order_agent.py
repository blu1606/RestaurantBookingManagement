import json
from typing import Dict, Any
from ..core.base_agent import BaseAgent
from ..utils import handle_pending_action_utils
from rapidfuzz import process, fuzz  # Thêm import fuzzy match

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
        self.menu_items = None  # Cache menu items

    def _load_menu_items(self):
        if self.menu_items is not None:
            return self.menu_items
        # Lấy từ knowledge_base
        self.menu_items = [item for item in self.knowledge_base if item.get("type") == "menu_item" or ("itemId" in item and "name" in item)]
        return self.menu_items

    def find_best_menu_match(self, item_name_or_id):
        menu_items = self._load_menu_items()
        # Nếu là số, tra cứu theo ID
        try:
            item_id = int(item_name_or_id)
            for item in menu_items:
                if str(item.get("itemId")) == str(item_id):
                    return [item]
        except Exception:
            pass
        # Fuzzy match theo tên
        names = [item["name"] for item in menu_items]
        best_match = process.extractOne(item_name_or_id, names, scorer=fuzz.token_sort_ratio)
        if best_match and best_match[1] > 90:
            idx = names.index(best_match[0])
            return [menu_items[idx]]
        elif best_match and best_match[1] > 70:
            matches = process.extract(item_name_or_id, names, scorer=fuzz.token_sort_ratio, limit=3)
            return [menu_items[names.index(m[0])] for m in matches if m[1] > 70]
        else:
            return []

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
        # Nếu không có tool, trả lời tự nhiên dựa vào context/knowledge base
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
        Dùng AI để phân tích user_input và trả về params (orderId, itemName/name, quantity)
        """
        import re
        prompt = f"""
        Phân tích yêu cầu sau và trả về JSON với các trường:
        {{
          \"orderId\": (số, nếu có),
          \"itemName\": (tên món, nếu có),
          \"name\": (tên món, nếu có),
          \"quantity\": (số lượng, nếu có)
        }}
        Nếu không có trường nào thì để null.
        Yêu cầu: \"{user_input}\"
        """
        response_text = self._call_gemini(prompt)
        # Improved markdown code block removal
        response_text = response_text.strip()
        if response_text.startswith("```"):
            # Remove opening ```json or ``` and closing ```
            response_text = re.sub(r'^```[a-zA-Z]*\s*', '', response_text)
            response_text = re.sub(r'```\s*$', '', response_text)
            response_text = response_text.strip()
        try:
            params = json.loads(response_text)
            required = tool.get("parameters", [])
            # Ưu tiên lấy itemName
            item_name = params.get("itemName") or params.get("name")
            if item_name:
                matches = self.find_best_menu_match(item_name)
                if len(matches) == 1:
                    params["itemName"] = matches[0]["name"]
                    params["itemId"] = matches[0]["itemId"]
                elif len(matches) > 1:
                    # Gợi ý danh sách món gần đúng
                    params["itemName_candidates"] = [{"name": m["name"], "itemId": m["itemId"]} for m in matches]
                else:
                    params["itemName_candidates"] = []
            return {k: v for k, v in params.items() if v is not None}
        except Exception as e:
            print(f"[OrderAgent] Lỗi parse params từ AI: {e} | raw: {response_text}")
            return {}
    
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
        Hỏi thêm thông tin thiếu cho order, hỗ trợ gợi ý món gần đúng
        """
        tool_name = tool.get("name", "")
        response = ""
        if "add_item_to_order" in tool_name:
            questions = []
            if "orderId" in missing_params:
                questions.append("Bạn muốn thêm món vào đơn hàng nào? (VD: đơn hàng 5)")
            if "itemName" in missing_params:
                questions.append("Bạn muốn thêm món gì? (Bạn có thể nhập tên hoặc ID món ăn)")
            if "quantity" in missing_params:
                questions.append("Bạn muốn thêm bao nhiêu phần?")
            response = f"Để thêm món vào đơn hàng, tôi cần thêm thông tin:\n"
            response += "\n".join([f"- {q}" for q in questions])
            response += "\n\nBạn có thể cung cấp thông tin này không?"
        # Nếu có gợi ý món gần đúng
        elif "itemName_candidates" in tool:
            candidates = tool["itemName_candidates"]
            if candidates:
                response = "Tôi tìm thấy các món gần giống: "
                response += ", ".join([f'{c["name"]} (ID: {c["itemId"]})' for c in candidates])
                response += ". Bạn muốn chọn món nào? (Nhập tên hoặc ID)"
            else:
                response = "Không tìm thấy món ăn phù hợp. Bạn vui lòng nhập lại tên hoặc ID món ăn."
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