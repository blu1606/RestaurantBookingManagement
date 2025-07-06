import json
from typing import Dict, Any, Optional
try:
    from ..core.base_agent import BaseAgent
except ImportError:
    try:
        from agents.core.base_agent import BaseAgent
    except ImportError:
        # Fallback for direct execution
        from base_agent import BaseAgent

class MenuAgent(BaseAgent):
    """
    AI Agent chuyên xử lý gợi ý món ăn và thông tin menu
    """
    
    def __init__(self, gemini_model=None):
        super().__init__(
            agent_name="MenuAgent",
            data_files=["menu_items.json"],
            gemini_model=gemini_model,
            service_type=None  # Không filter theo service, chỉ dùng show_menu
        )
    
    def get_system_prompt(self) -> str:
        return """
        Bạn là nhân viên tư vấn menu chuyên nghiệp của nhà hàng Việt Nam.
        Nhiệm vụ chính của bạn là:
        1. Gợi ý món ăn phù hợp với sở thích và nhu cầu khách hàng
        2. Cung cấp thông tin chi tiết về món ăn (nguyên liệu, cách chế biến, giá cả)
        3. Phân loại món ăn theo loại (chính, tráng miệng, đồ uống)
        4. Giới thiệu các món đặc sản và món phổ biến
        5. Tư vấn món ăn theo tầm giá và số lượng người
        6. Chỉ sử dụng tool show_menu khi khách hàng yêu cầu xem toàn bộ menu
        
        Luôn đảm bảo thông tin chính xác và tư vấn nhiệt tình.
        """
    
    def process_request(self, user_input: str, session_id: str = "default", chat_session=None) -> Dict[str, Any]:
        # 1. Check if user_input matches show_menu tool specifically
        tool = self._detect_show_menu_tool(user_input)
        if tool:
            # Extract parameters (simple: just return empty or all None, real use: NLP extract)
            parameters = {param: None for param in tool.get("parameters", [])}
            return self.create_response(
                action=tool["name"],
                parameters=parameters,
                natural_response=f"Tôi sẽ thực hiện tác vụ: {tool['description']} (service: {tool['service']})"
            )
        
        # 2. Fallback: Lấy context từ knowledge base (menu items) và trả lời tự nhiên
        context = self._get_relevant_context(user_input)
        prompt = f"""
        {self.get_system_prompt()}
        
        Thông tin về menu nhà hàng:
        {context}
        
        Yêu cầu của khách hàng: {user_input}
        
        Hãy trả lời một cách thân thiện và hữu ích. Nếu khách hàng hỏi về:
        - Gợi ý món ăn: Đưa ra gợi ý dựa trên menu, giải thích tại sao phù hợp
        - Món phổ biến: Liệt kê các món được ưa chuộng với lý do
        - Món theo giá: Gợi ý món theo tầm giá (dưới 50k, 50k-100k, trên 100k)
        - Món theo loại: Phân loại món (chính, tráng miệng, đồ uống) với mô tả
        - Thông tin món: Mô tả chi tiết về món ăn, nguyên liệu, cách chế biến
        - Món đặc sản: Giới thiệu các món đặc trưng của nhà hàng
        - Món theo số người: Tư vấn món phù hợp theo số lượng khách
        
        Trả lời bằng tiếng Việt, thân thiện và chi tiết. Nếu cần xem toàn bộ menu, hãy gợi ý sử dụng tool show_menu.
        """
        response_text = self._call_gemini(prompt, chat_session=chat_session)
        response_data = self._parse_json_response(
            response_text,
            fallback_action="menu_suggestion",
            fallback_response="Tôi sẽ giúp bạn với thông tin menu. Bạn cần hỗ trợ gì?"
        )
        return self.create_response(
            action=response_data.get("action", "menu_suggestion"),
            parameters=response_data.get("parameters", {}),
            natural_response=response_data.get("naturalResponse", "Tôi sẽ giúp bạn với thông tin menu.")
        )
    
    def _detect_show_menu_tool(self, user_input: str) -> Optional[Dict[str, Any]]:
        """
        Chỉ detect tool show_menu, bỏ qua các tools khác
        """
        if not self.tool_detector:
            return None
        
        # Chỉ tìm tool show_menu
        show_menu_tool = None
        for tool in self.tool_detector.tools:
            if tool.get("name") == "show_menu":
                show_menu_tool = tool
                break
        
        if not show_menu_tool:
            return None
        
        # Kiểm tra xem user input có match với show_menu không
        user_input_lower = user_input.lower()
        show_menu_keywords = ["xem menu", "cho xem menu", "hiển thị menu", "menu", "danh sách món"]
        
        for keyword in show_menu_keywords:
            if keyword in user_input_lower:
                return show_menu_tool
        
        return None
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format menu knowledge item
        """
        if isinstance(item, dict):
            if item.get("type") == "menu_item":
                return f"Món: {item.get('name', '')} - Giá: {item.get('price', '')} VND - Mô tả: {item.get('description', '')}"
            elif item.get("type") == "category":
                return f"Danh mục: {item.get('name', '')} - {item.get('description', '')}"
            elif item.get("type") == "special":
                return f"Món đặc biệt: {item.get('name', '')} - {item.get('description', '')}"
        return super()._format_knowledge_item(item) 