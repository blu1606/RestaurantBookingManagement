import os
import json
from abc import ABC, abstractmethod
from typing import Dict, Any, List, Optional
from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_google_genai import GoogleGenerativeAIEmbeddings
import google.generativeai as genai
import pickle


from ..utils.rate_limiter import RateLimiter
from ..utils.tool_detector import ToolDetector
from ..utils.data_tool_manager import DataToolManager


class BaseAgent(ABC):
    """
    Base class cho tất cả AI Agent chuyên biệt
    """
    
    def __init__(self, agent_name: str, data_files: list = None, gemini_model=None, 
                 service_type: str = None, allowed_tools: list = None, user_role: str = "user"):
        self.agent_name = agent_name
        self.data_files = data_files or []
        self.gemini_model = gemini_model
        self.service_type = service_type  # Giữ lại để backward compatibility
        self.user_role = user_role  # Role của user (user, staff, admin)
        self.vector_db = None
        self.retriever = None
        self.knowledge_base = []
        self.rate_limiter = RateLimiter()
        self.data_tool_manager = DataToolManager()
        
        # Load permissions trước
        self._load_permissions()
        
        # Load tools trước
        self._load_tools()
        
        # Load knowledge base
        self._load_knowledge_base()
        
        # Build vector database
        self._build_vector_db()
    
    def _load_permissions(self):
        """
        Load permissions từ file agent_permissions.json
        """
        try:
            permissions_path = os.path.join(os.path.dirname(__file__), "..", "..", "agent_permissions.json")
            if not os.path.exists(permissions_path):
                print(f"⚠️ {self.agent_name}: Permissions file not found, using default permissions")
                self.allowed_tools = []
                return
            
            with open(permissions_path, 'r', encoding='utf-8') as f:
                permissions_data = json.load(f)
            
            # Lấy permissions cho agent này
            agent_permissions = permissions_data.get("agents", {}).get(self.agent_name, {})
            self.allowed_tools = agent_permissions.get("allowed_tools", [])
            
            # Lấy role permissions
            role_permissions = permissions_data.get("roles", {}).get(self.user_role, {})
            self.role_permissions = role_permissions.get("permissions", [])
            
            print(f"✅ {self.agent_name}: Loaded permissions for role '{self.user_role}'")
            print(f"✅ {self.agent_name}: Allowed tools: {self.allowed_tools}")
            
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error loading permissions: {e}")
            self.allowed_tools = []
            self.role_permissions = []
    
    def _load_tools(self):
        """
        Load tools từ file tools_customer.json (chỉ tools dành cho customer)
        """
        try:
            tools_path = os.path.join(os.path.dirname(__file__), "..", "..", "tools_customer.json")
            if not os.path.exists(tools_path):
                tools_path = os.path.join(os.path.dirname(__file__), "..", "..", "tools.json")
            # Lấy tools đã lọc từ DataToolManager (cache theo role/allowed_tools)
            filtered_tools = self.data_tool_manager.get_filtered_tools(
                tools_path,
                self.user_role,
                self.allowed_tools,
                getattr(self, 'role_permissions', None)
            )
            print(f"✅ {self.agent_name}: Filtered {len(filtered_tools)} tools for role '{self.user_role}' from {len(filtered_tools)} total tools")
            self.tool_detector = ToolDetector.get_instance(filtered_tools)
            print(f"✅ {self.agent_name}: Loaded {len(filtered_tools)} tools from {os.path.basename(tools_path)}")
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error loading tools: {e}")
            self.tool_detector = None
    
    def _load_knowledge_base(self):
        """
        Load knowledge base từ các file data JSON
        """
        try:
            self.knowledge_base = []
            # Luôn lấy data_dir là thư mục data ở project root
            project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
            data_dir = os.path.join(project_root, "data")
            if not os.path.exists(data_dir):
                print(f"🔥 {self.agent_name}: Could not find data directory at {data_dir}")
                return
            for data_file in self.data_files:
                clean_data_file = data_file.replace("../", "").replace("../../", "")
                abs_data_path = os.path.join(data_dir, clean_data_file)
                if os.path.exists(abs_data_path):
                    data = self.data_tool_manager.load_data(clean_data_file, [data_dir])
                    for item in data:
                        item['_source_file'] = data_file
                    self.knowledge_base.extend(data)
                    print(f"✅ {self.agent_name}: Loaded data from {data_file}")
                else:
                    print(f"⚠️ {self.agent_name}: Data file {data_file} not found at {abs_data_path}")
            if self.knowledge_base:
                print(f"✅ {self.agent_name}: Loaded {len(self.knowledge_base)} items from data files")
            else:
                print(f"⚠️ {self.agent_name}: No data files loaded")
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error loading knowledge base: {e}")
    
    def _build_vector_db(self):
        """
        Xây dựng hoặc load vector database từ knowledge base, sử dụng batch embedding
        """
        try:
            if not self.knowledge_base:
                return
            # Đường dẫn cache vector DB
            project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
            cache_dir = os.path.join(project_root, "data", "vector_db_cache")
            os.makedirs(cache_dir, exist_ok=True)
            cache_file = os.path.join(cache_dir, f"{self.agent_name}_vector_db.pkl")
            # Nếu đã có file cache, load lại
            if os.path.exists(cache_file):
                with open(cache_file, 'rb') as f:
                    self.vector_db = pickle.load(f)
                self.retriever = self.vector_db.as_retriever(search_kwargs={"k": 3})
                print(f"✅ {self.agent_name}: Loaded vector DB from cache {cache_file}")
                return
            # Nếu chưa có, build mới với batch embedding
            documents = []
            texts = []
            for item in self.knowledge_base:
                content = self._format_knowledge_item(item)
                metadata = {"source": self.agent_name, "type": item.get("type", "general")}
                documents.append(Document(page_content=content, metadata=metadata))
                texts.append(content)
            if documents:
                embeddings = self.data_tool_manager.get_embeddings_batch(texts)
                from langchain_chroma import Chroma
                self.vector_db = Chroma.from_embeddings(texts, embeddings, metadatas=[doc.metadata for doc in documents])
                self.retriever = self.vector_db.as_retriever(search_kwargs={"k": 3})
                # Lưu lại vector DB vào cache
                with open(cache_file, 'wb') as f:
                    pickle.dump(self.vector_db, f)
                print(f"✅ {self.agent_name}: Vector DB built (batch) and cached with {len(documents)} documents at {cache_file}")
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error building/loading vector DB: {e}")
    
    def _format_knowledge_item(self, item: Dict[str, Any]) -> str:
        """
        Format knowledge item thành text (có thể override trong subclass)
        """
        if isinstance(item, dict):
            return json.dumps(item, ensure_ascii=False)
        return str(item)
    
    def _get_relevant_context(self, user_input: str) -> str:
        """
        Lấy context liên quan từ vector database
        """
        if not self.retriever:
            return "Không có thông tin bổ sung."
        
        try:
            relevant_docs = self.retriever.invoke(user_input)
            context = "\n".join([doc.page_content for doc in relevant_docs])
            return context if context else "Không có thông tin bổ sung."
        except Exception as e:
            print(f"⚠️ {self.agent_name}: Error retrieving context: {e}")
            return "Không có thông tin bổ sung."
    
    def _call_gemini(self, prompt: str, chat_session: Optional[Any] = None) -> str:
        """
        Gọi Gemini API, có thể sử dụng chat_session để duy trì ngữ cảnh
        """
        if not self.gemini_model:
            return "Xin lỗi, AI service hiện không khả dụng."
        
        # Apply rate limiting
        self.rate_limiter.wait_if_needed()
        
        try:
            if chat_session:
                # Nếu có chat_session, gửi message vào session
                response = chat_session.send_message(prompt)
            else:
                # Nếu không, tạo một phiên độc lập
                response = self.gemini_model.generate_content(prompt)
            return response.text
        except Exception as e:
            print(f"🔥 {self.agent_name}: Error calling Gemini: {e}")
            return "Xin lỗi, có lỗi xảy ra khi xử lý yêu cầu."
    
    def _vector_db_cache_file(self):
        project_root = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
        cache_dir = os.path.join(project_root, "data", "vector_db_cache")
        os.makedirs(cache_dir, exist_ok=True)
        return os.path.join(cache_dir, f"{self.agent_name}_vector_db.pkl")

    def refresh_knowledge(self):
        """
        Refresh knowledge base từ file và xóa cache vector DB nếu có
        """
        # Xóa cache vector DB nếu tồn tại
        cache_file = self._vector_db_cache_file()
        if os.path.exists(cache_file):
            try:
                os.remove(cache_file)
                print(f"🗑️ {self.agent_name}: Removed vector DB cache {cache_file}")
            except Exception as e:
                print(f"⚠️ {self.agent_name}: Failed to remove vector DB cache: {e}")
        self._load_knowledge_base()
    
    @abstractmethod
    def get_system_prompt(self) -> str:
        """
        Trả về system prompt cho agent này
        """
        pass
    
    @abstractmethod
    def process_request(self, user_input: str, session_id: str = "default", chat_session: Optional[Any] = None) -> Dict[str, Any]:
        """
        Xử lý yêu cầu của user
        """
        pass
    
    def create_response(self, action: str, parameters: Dict[str, Any], 
                       natural_response: str) -> Dict[str, Any]:
        """
        Tạo response chuẩn với xử lý actions từ Java services
        """
        # Clean parameters to avoid null values
        cleaned_parameters = {}
        for key, value in parameters.items():
            if value is not None and value != "null" and value != "":
                cleaned_parameters[key] = value
        
        # Check if this is a Java service action that needs special handling
        java_service_actions = [
            "add_menu", "delete_menu", "update_menu", "add_item_to_order", "remove_item_from_order",
            "complete_order", "calculate_bill", "get_revenue", "add_table", "delete_table", 
            "update_table", "search_tables", "show_available_tables", "show_all_tables", "create_booking", "cancel_booking", "complete_booking",
            "update_booking", "delete_booking", "create_customer", "update_customer", "delete_customer",
            "get_customer_info", "customer_search", "show_menu", "fix_data"
        ]
        
        if action in java_service_actions:
            # Lấy service type từ tool definition
            service_type = self._get_service_type_from_action(action)
            if service_type:
                # Add metadata to indicate this should be handled by Java service
                return {
                    "action": action,
                    "parameters": cleaned_parameters,
                    "naturalResponse": natural_response,
                    "agent": self.agent_name,
                    "requiresJavaService": True,
                    "javaServiceType": service_type
                }
        
        return {
            "action": action,
            "parameters": cleaned_parameters,
            "naturalResponse": natural_response,
            "agent": self.agent_name
        }
    
    def _get_service_type_from_action(self, action: str) -> str:
        """
        Lấy service type từ tool definition thay vì hardcode mapping
        """
        if not self.tool_detector:
            return None
        
        for tool in self.tool_detector.tools:
            if tool.get("name") == action:
                return tool.get("service")
        
        return None
    
    def _parse_json_response(self, response_text: str, fallback_action: str = "clarify", 
                           fallback_response: str = "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại.") -> Dict[str, Any]:
        """
        Parse JSON response from Gemini with improved error handling
        """
        try:
            # First, try to parse as-is
            parsed = json.loads(response_text)
            
            # Clean up null values to prevent JsonNull errors
            if isinstance(parsed, dict):
                # Clean parameters
                if "parameters" in parsed and isinstance(parsed["parameters"], dict):
                    cleaned_params = {}
                    for key, value in parsed["parameters"].items():
                        if value is not None and value != "null" and value != "":
                            cleaned_params[key] = value
                    parsed["parameters"] = cleaned_params
                
                # Ensure required fields exist
                if "action" not in parsed:
                    parsed["action"] = fallback_action
                if "naturalResponse" not in parsed:
                    parsed["naturalResponse"] = fallback_response
                if "parameters" not in parsed:
                    parsed["parameters"] = {}
            
            return parsed
        except json.JSONDecodeError:
            # Try to extract JSON from the response using regex
            import re
            json_match = re.search(r'\{.*\}', response_text, re.DOTALL)
            if json_match:
                try:
                    parsed = json.loads(json_match.group())
                    # Clean up null values
                    if isinstance(parsed, dict):
                        if "parameters" in parsed and isinstance(parsed["parameters"], dict):
                            cleaned_params = {}
                            for key, value in parsed["parameters"].items():
                                if value is not None and value != "null" and value != "":
                                    cleaned_params[key] = value
                            parsed["parameters"] = cleaned_params
                    return parsed
                except json.JSONDecodeError:
                    pass
            
            # If all parsing attempts fail, return fallback
            print(f"⚠️ {self.agent_name}: Failed to parse JSON response: {response_text[:100]}...")
            return {
                "action": fallback_action,
                "parameters": {},
                "naturalResponse": fallback_response
            }
    
    def detect_tool_from_prompt(self, user_input: str) -> Optional[Dict[str, Any]]:
        """
        Detect if user_input matches any tool using semantic similarity
        Chỉ xử lý tools được phép sử dụng (permission-based)
        """
        if not self.tool_detector:
            return None
        
        # Lọc tools theo agent allowed_tools và role permissions
        available_tools = []
        for tool in self.tool_detector.tools:
            # Kiểm tra agent allowed_tools
            if self.allowed_tools and tool.get("name") not in self.allowed_tools:
                continue
            
            # Kiểm tra role permissions từ agent_permissions.json
            if hasattr(self, 'role_permissions') and self.role_permissions:
                if tool.get("name") not in self.role_permissions:
                    continue
            
            available_tools.append(tool)
        
        print(f"🔍 {self.agent_name}: Checking {len(available_tools)} tools for role '{self.user_role}'")
        if self.allowed_tools:
            print(f"🔍 {self.agent_name}: Allowed tools: {self.allowed_tools}")
        if hasattr(self, 'role_permissions') and self.role_permissions:
            print(f"🔍 {self.agent_name}: Role permissions: {self.role_permissions}")
        
        # Tạo temporary tool detector với filtered tools
        temp_detector = ToolDetector(available_tools)
        
        return temp_detector.detect_tool_from_prompt(user_input) 