import google.generativeai as genai
import json
import os
import re
from datetime import datetime, timedelta
from typing import Dict, Any, List
from collections import defaultdict

from flask import Flask, request, jsonify
from flask_cors import CORS

from langchain_chroma import Chroma
from langchain_core.documents import Document
from langchain_google_genai import GoogleGenerativeAIEmbeddings

# --- Step 1: Setup and Configuration ---

# Load environment variables from .env if available
try:
    from dotenv import load_dotenv
    load_dotenv()
    print("✅ .env file loaded.")
except ImportError:
    print("⚠️ Warning: python-dotenv not installed. .env file will not be loaded.")

app = Flask(__name__)
CORS(app)

# Session management for conversation history
session_data = defaultdict(dict)
conversation_history = defaultdict(list)

# Configure Gemini API once at startup
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
gemini_model = None
if GOOGLE_API_KEY:
    try:
        genai.configure(api_key=GOOGLE_API_KEY)
        gemini_model = genai.GenerativeModel('gemini-2.5-flash')
        print("✅ Gemini API configured successfully.")
    except Exception as e:
        print(f"🔥 Error configuring Gemini API: {e}")
        gemini_model = None
else:
    print("⚠️ GOOGLE_API_KEY not found. Running in Fallback Mode.")

# --- Step 2: Knowledge Base Setup (The "R" in RAG) ---
# We use ChromaDB for a simple, file-based vector store.
# In a real app, this data would come from a database.
try:
    
    
    # Global variables for knowledge base
    vector_db = None
    retriever = None
    
    # Path to data files
    DATA_DIR = "../data"
    
    def load_knowledge_from_files():
        """Đọc tất cả file JSON và xây dựng lại VectorDB từ đầu."""
        global vector_db, retriever
        print("🔄 Starting to load/reload knowledge from files...")
        
        try:
            all_docs = []
            
            # Load menu items
            menu_file = os.path.join(DATA_DIR, 'menu_items.json')
            if os.path.exists(menu_file):
                with open(menu_file, 'r', encoding='utf-8') as f:
                    menu_data = json.load(f)
                
                for item in menu_data:
                    content = f"Món {item['name']}: {item['description']}. Giá: {item['price']} VND."
                    all_docs.append(Document(page_content=content, metadata={"source": "menu", "itemId": item['itemId']}))
                print(f"📋 Loaded {len(menu_data)} menu items")
            
            # Load tables
            tables_file = os.path.join(DATA_DIR, 'tables.json')
            if os.path.exists(tables_file):
                with open(tables_file, 'r', encoding='utf-8') as f:
                    tables_data = json.load(f)
                
                for table in tables_data:
                    content = f"Bàn số {table['tableId']} cho {table['capacity']} người. Trạng thái hiện tại: {table['status']}."
                    all_docs.append(Document(page_content=content, metadata={"source": "table_status", "tableId": table['tableId']}))
                print(f"🪑 Loaded {len(tables_data)} tables")
            
            # Load bookings
            bookings_file = os.path.join(DATA_DIR, 'bookings.json')
            if os.path.exists(bookings_file):
                with open(bookings_file, 'r', encoding='utf-8') as f:
                    bookings_data = json.load(f)
                
                for booking in bookings_data:
                    content = f"Đặt bàn số {booking['bookingId']} cho {booking['numberOfGuests']} người vào {booking['bookingTime']}. Trạng thái: {booking['status']}."
                    all_docs.append(Document(page_content=content, metadata={"source": "booking", "bookingId": booking['bookingId']}))
                print(f"📅 Loaded {len(bookings_data)} bookings")
            
            # Load customers
            customers_file = os.path.join(DATA_DIR, 'customers.json')
            if os.path.exists(customers_file):
                with open(customers_file, 'r', encoding='utf-8') as f:
                    customers_data = json.load(f)
                
                for customer in customers_data:
                    content = f"Khách hàng {customer['name']} (ID: {customer['customerId']}) - SĐT: {customer['phone']}, Email: {customer['email']}."
                    all_docs.append(Document(page_content=content, metadata={"source": "customer", "customerId": customer['customerId']}))
                print(f"👥 Loaded {len(customers_data)} customers")
            
            # Add static restaurant information
            static_docs = [
                Document(page_content="Giờ mở cửa của nhà hàng là từ 9 giờ sáng đến 10 giờ tối (22:00) hàng ngày.", metadata={"source": "info"}),
                Document(page_content="Để hủy đặt bàn, khách hàng cần cung cấp mã đặt bàn (bookingId).", metadata={"source": "policy"}),
                Document(page_content="Nhà hàng có các loại bàn dành cho 2 người, 4 người, 6 người và 8 người.", metadata={"source": "info"}),
            ]
            all_docs.extend(static_docs)
            
            if all_docs:
                embeddings = GoogleGenerativeAIEmbeddings(model="models/embedding-001")
                vector_db = Chroma.from_documents(all_docs, embeddings)
                retriever = vector_db.as_retriever(search_kwargs={"k": 3})  # Retrieve top 3 relevant docs
                print(f"✅ Knowledge base has been successfully (re)built with {len(all_docs)} documents.")
                return True
            else:
                print("⚠️ No documents found to build knowledge base.")
                return False
                
        except Exception as e:
            print(f"🔥 Failed to build knowledge base: {e}")
            return False

    # Initial load of knowledge base
    if load_knowledge_from_files():
        print("✅ ChromaDB knowledge base created from files.")
    else:
        print("⚠️ Failed to create knowledge base from files. Using fallback mode.")
        retriever = None

except ImportError:
    print("⚠️ LangChain or ChromaDB not installed. Retrieval functionality will be disabled.")
    retriever = None
except Exception as e:
    print(f"🔥 Error creating ChromaDB knowledge base: {e}")
    retriever = None

# --- Step 3: LLM Interaction with Improved Prompt ---

def get_current_vietnam_time() -> str:
    """Returns the current date and time in Vietnam."""
    # Simulating for the example date
    return "Chủ Nhật, ngày 29 tháng 06 năm 2025, lúc 10:37 sáng."


def call_gemini_with_rag(user_input: str, session_id: str = "default", role: str = "USER") -> Dict[str, Any]:
    """
    Calls the Gemini API with a dynamically retrieved context and conversation history.
    """
    if not gemini_model:
        raise ConnectionError("Gemini API not configured.")

    # 1. Retrieve relevant context
    context = "Không có thông tin bổ sung."
    if retriever:
        relevant_docs = retriever.invoke(user_input)
        context = "\n".join([doc.page_content for doc in relevant_docs])

    # 2. Get conversation history for this session
    history = conversation_history.get(session_id, [])
    history_text = ""
    if history:
        history_text = "\n\nLỊCH SỬ HỘI THOẠI:\n" + "\n".join(history[-5:])  # Last 5 exchanges

    # 3. Create a powerful few-shot prompt with conversation history
    prompt = f"""
    Bạn là một trợ lý AI thông minh của một nhà hàng Việt Nam.
    Nhiệm vụ của bạn là phân tích yêu cầu của khách hàng và trả về một chuỗi JSON duy nhất.
    Không thêm bất kỳ giải thích hay lời chào nào khác. Chỉ trả về JSON.
    QUAN TRỌNG: Không escape Unicode characters trong JSON response.

    Thời gian hiện tại là: {get_current_vietnam_time()}
    Role hiện tại: {role}

    Dưới đây là một số thông tin có thể hữu ích từ cơ sở tri thức của nhà hàng:
    --- CONTEXT ---
    {context}
    --- END CONTEXT ---

    {history_text}

    Hãy tuân thủ nghiêm ngặt định dạng JSON sau với các trường: "action", "parameters", và "naturalResponse".

    CÁC ACTION CÓ SẴN:
    1. "show_menu" - Hiển thị menu nhà hàng
    2. "show_tables" - Hiển thị trạng thái các bàn
    3. "show_bookings" - Hiển thị danh sách đặt bàn
    4. "book_table" - Đặt bàn (khi đã có đủ thông tin)
    5. "collect_customer_info" - Thu thập thông tin khách hàng trước khi đặt bàn
    6. "order_food" - Đặt món ăn
    7. "calculate_bill" - Tính bill cho bàn hiện tại
    8. "cancel_booking" - Hủy đặt bàn
    9. "clarify" - Yêu cầu làm rõ thêm

    CÁC ACTION CHỈ DÀNH CHO MANAGER:
    10. "add_menu" - Thêm món ăn mới vào menu
    11. "delete_menu" - Xóa món ăn khỏi menu
    12. "add_table" - Thêm bàn mới
    13. "delete_booking" - Xóa đặt bàn
    14. "fix_data" - Sửa lỗi dữ liệu
    15. "show_customers" - Hiển thị danh sách khách hàng
    16. "customer_info" - Xem thông tin khách hàng
    17. "customer_search" - Tìm kiếm khách hàng

    LƯU Ý QUAN TRỌNG VỀ ROLE:
    - USER: Chỉ có thể thực hiện các action từ 1-8 (xem thông tin, đặt bàn, gọi món, hủy đặt bàn)
    - MANAGER: Có thể thực hiện tất cả các action từ 1-17 (bao gồm cả quản lý CRUD)
    - Nếu USER yêu cầu thực hiện action chỉ dành cho MANAGER, trả về action "clarify" với thông báo phù hợp

    LƯU Ý QUAN TRỌNG VỀ CONVERSATION HISTORY:
    - Nếu khách hàng đã cung cấp thông tin trong lịch sử hội thoại, sử dụng thông tin đó
    - Không hỏi lại thông tin đã có
    - Kết hợp thông tin từ các tin nhắn trước để hiểu yêu cầu đầy đủ
    - Nếu đã có đủ thông tin (tên, số điện thoại, số người, thời gian), tiến hành đặt bàn

    Ví dụ 1: Yêu cầu xem menu (USER/MANAGER)
    Khách hàng: "cho tôi xem menu"
    JSON:
    {{
        "action": "show_menu",
        "parameters": {{}},
        "naturalResponse": "Dạ vâng, đây là menu của nhà hàng ạ."
    }}

    Ví dụ 2: Đặt bàn với thông tin đầy đủ (USER/MANAGER)
    Khách hàng: "đặt bàn 3 người 7h tối, tên tôi là Nguyễn Văn A, số điện thoại 0901234567"
    JSON:
    {{
        "action": "book_table",
        "parameters": {{
            "guests": 3,
            "time": "2025-06-29T19:00:00",
            "customerName": "Nguyễn Văn A",
            "customerPhone": "0901234567"
        }},
        "naturalResponse": "Dạ vâng, em đã ghi nhận thông tin của anh Nguyễn Văn A. Em sẽ tiến hành đặt bàn cho 3 người vào 7 giờ tối nay ạ."
    }}

    Ví dụ 3: Cung cấp thông tin bổ sung (USER/MANAGER)
    Khách hàng: "tên tôi là Trần Thị B, số điện thoại 0987654321"
    JSON:
    {{
        "action": "collect_customer_info",
        "parameters": {{
            "customerName": "Trần Thị B",
            "customerPhone": "0987654321"
        }},
        "naturalResponse": "Dạ vâng, em đã ghi nhận thông tin của chị Trần Thị B. Em cần thêm thông tin về số người và thời gian đặt bàn ạ."
    }}

    Ví dụ 4: Tính bill (USER/MANAGER)
    Khách hàng: "tính bill" hoặc "tính tiền"
    JSON:
    {{
        "action": "calculate_bill",
        "parameters": {{}},
        "naturalResponse": "Dạ vâng, em sẽ tính bill cho bàn hiện tại của anh/chị ạ."
    }}

    Ví dụ 5: Thêm món ăn mới (CHỈ MANAGER)
    Khách hàng: "thêm món phở bò giá 45000 vào menu"
    JSON:
    {{
        "action": "add_menu",
        "parameters": {{
            "name": "Phở Bò",
            "price": 45000,
            "description": "Món phở truyền thống Việt Nam"
        }},
        "naturalResponse": "Dạ vâng, em sẽ thêm món Phở Bò với giá 45,000 VND vào menu ạ."
    }}

    Ví dụ 6: Xóa món ăn (CHỈ MANAGER)
    Khách hàng: "xóa món số 3 khỏi menu"
    JSON:
    {{
        "action": "delete_menu",
        "parameters": {{
            "itemId": 3
        }},
        "naturalResponse": "Dạ vâng, em sẽ xóa món ăn số 3 khỏi menu ạ."
    }}

    Ví dụ 7: Thêm bàn mới (CHỈ MANAGER)
    Khách hàng: "thêm bàn mới cho 6 người"
    JSON:
    {{
        "action": "add_table",
        "parameters": {{
            "capacity": 6
        }},
        "naturalResponse": "Dạ vâng, em sẽ thêm bàn mới cho 6 người ạ."
    }}

    Ví dụ 8: Xóa đặt bàn (CHỈ MANAGER)
    Khách hàng: "xóa đặt bàn số 5"
    JSON:
    {{
        "action": "delete_booking",
        "parameters": {{
            "bookingId": 5
        }},
        "naturalResponse": "Dạ vâng, em sẽ xóa đặt bàn số 5 ạ."
    }}

    Ví dụ 9: Sửa lỗi dữ liệu (CHỈ MANAGER)
    Khách hàng: "sửa lỗi dữ liệu"
    JSON:
    {{
        "action": "fix_data",
        "parameters": {{}},
        "naturalResponse": "Dạ vâng, em sẽ kiểm tra và sửa các lỗi dữ liệu ạ."
    }}

    Ví dụ 10: Xem danh sách khách hàng (CHỈ MANAGER)
    Khách hàng: "cho tôi xem danh sách khách hàng"
    JSON:
    {{
        "action": "show_customers",
        "parameters": {{}},
        "naturalResponse": "Dạ vâng, em sẽ hiển thị danh sách khách hàng ạ."
    }}

    Ví dụ 11: Tìm kiếm khách hàng (CHỈ MANAGER)
    Khách hàng: "tìm khách hàng tên Nguyễn"
    JSON:
    {{
        "action": "customer_search",
        "parameters": {{
            "searchTerm": "Nguyễn"
        }},
        "naturalResponse": "Dạ vâng, em sẽ tìm kiếm khách hàng có tên Nguyễn ạ."
    }}

    Ví dụ 12: USER yêu cầu thực hiện lệnh MANAGER
    Khách hàng: "thêm món mới vào menu"
    JSON:
    {{
        "action": "clarify",
        "parameters": {{}},
        "naturalResponse": "Xin lỗi, chức năng này chỉ dành cho quản lý. Bạn có thể liên hệ quản lý để thực hiện thao tác này ạ."
    }}

    LƯU Ý QUAN TRỌNG:
    - Khi khách hàng yêu cầu xem thông tin (menu, bàn, đặt bàn), sử dụng action tương ứng
    - Khi khách hàng muốn đặt bàn nhưng chưa cung cấp đủ thông tin, sử dụng action "collect_customer_info"
    - Khi khách hàng đã cung cấp đủ thông tin, sử dụng action "book_table"
    - Khi khách hàng muốn đặt món, sử dụng action "order_food"
    - Khi khách hàng muốn tính bill, sử dụng action "calculate_bill"
    - Khi khách hàng muốn hủy đặt bàn, sử dụng action "cancel_booking" với parameters gồm "bookingId" là ID đặt bàn (ví dụ: "123"). 
    - Khi không hiểu rõ yêu cầu, sử dụng action "clarify"
    - LUÔN KIỂM TRA ROLE TRƯỚC KHI THỰC HIỆN ACTION QUẢN LÝ
    - LUÔN KIỂM TRA LỊCH SỬ HỘI THOẠI TRƯỚC KHI HỎI LẠI THÔNG TIN
    - Khi khách hàng gọi món (order_food) mà chưa xác định bàn, hãy hỏi rõ: "Bạn muốn order cho bàn số mấy?".
    - Nếu khách hàng có nhiều bàn đang đặt, hãy liệt kê các bàn đó để khách hàng chọn.

    Bây giờ, hãy xử lý yêu cầu thực tế của khách hàng.

    Khách hàng: "{user_input}"
    JSON:
    """

    try:
        # Thêm timeout cho Gemini API call
        import time
        start_time = time.time()
        
        response = gemini_model.generate_content(prompt)
        
        # Kiểm tra thời gian thực thi
        elapsed_time = time.time() - start_time
        print(f"⏱️ Gemini API call took {elapsed_time:.2f} seconds")
        
        # Clean up the response to extract only the JSON part
        json_match = re.search(r'```json\s*([\s\S]*?)\s*```', response.text)
        if json_match:
            json_str = json_match.group(1)
            return json.loads(json_str)
        else:
            # Fallback if LLM doesn't return the expected format
            return json.loads(response.text.strip())
    except Exception as e:
        print(f"🔥 Error during Gemini API call or JSON parsing: {e}")
        raise

def detect_back_to_menu(user_input: str) -> dict:
    """
    Nhận diện các lệnh quay về menu từ user input.
    Nếu phát hiện, trả về action show_menu.
    """
    menu_keywords = [
        "menu", "quay lại menu", "trở về menu", "back to menu", "main menu", "về menu", "quay lại", "trở về"
    ]
    normalized = user_input.strip().lower()
    for kw in menu_keywords:
        if kw in normalized:
            return {
                "action": "show_menu",
                "parameters": {},
                "naturalResponse": "Dạ vâng, đây là menu của nhà hàng ạ."
            }
    return None

# --- Step 4: Flask API Routes ---

@app.route('/process', methods=['POST'])
def process_user_input():
    """Main API endpoint to process user input."""
    try:
        data = request.get_json()
        if not data or 'userInput' not in data:
            return jsonify({"error": "Invalid input. 'userInput' field is required."}), 400

        user_input = data['userInput']
        session_id = data.get('sessionId', 'default')  # Get session ID from request
        role = data.get('role', 'USER')  # Get role from request, default to USER
        
        print(f"➡️ Received input from session {session_id} (Role: {role}): {user_input}")

        # Ưu tiên kiểm tra lệnh quay về menu
        menu_result = detect_back_to_menu(user_input)
        if menu_result:
            return jsonify(menu_result)

        # Add user input to conversation history
        conversation_history[session_id].append(f"Khách hàng: {user_input}")

        # Always try to use the intelligent RAG-based approach first
        if gemini_model:
            try:
                response_data = call_gemini_with_rag(user_input, session_id, role)
                
                # Add AI response to conversation history
                if 'naturalResponse' in response_data:
                    conversation_history[session_id].append(f"AI: {response_data['naturalResponse']}")
                
                # Keep only last 10 exchanges to avoid memory issues
                if len(conversation_history[session_id]) > 10:
                    conversation_history[session_id] = conversation_history[session_id][-10:]
                
                print(f"✅ Gemini RAG response for session {session_id} (Role: {role}): {response_data}")
                return jsonify(response_data)
            except Exception as e:
                print(f"⚠️ RAG call failed for session {session_id}: {e}. No fallback available for this complex task.")
                return jsonify({
                    "action": "clarify",
                    "parameters": {},
                    "naturalResponse": "Xin lỗi, hệ thống AI đang gặp sự cố nhỏ. Bạn có thể thử lại sau giây lát được không ạ?"
                }), 500
        else:
            # Simple fallback if Gemini is not configured at all
            print("⚠️ Using basic fallback mode as Gemini is not configured.")
            return jsonify({
                "action": "clarify",
                "parameters": {},
                "naturalResponse": "Xin lỗi, chế độ AI thông minh hiện không khả dụng. Vui lòng thử lại sau."
            })

    except Exception as e:
        print(f"🔥 Critical error in /process endpoint: {e}")
        return jsonify({"error": "An internal server error occurred."}), 500

@app.route('/refresh-knowledge', methods=['POST'])
def refresh_knowledge():
    """Endpoint để Java thông báo rằng dữ liệu đã thay đổi."""
    try:
        if load_knowledge_from_files():
            return jsonify({"status": "success", "message": "Knowledge base refreshed."}), 200
        else:
            return jsonify({"status": "error", "message": "Failed to refresh knowledge base."}), 500
    except Exception as e:
        print(f"🔥 Error in refresh-knowledge endpoint: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    return jsonify({
        "status": "healthy",
        "message": "AI Agent is running",
        "gemini_api_status": "available" if gemini_model else "not_configured",
        "knowledge_base_status": "loaded" if retriever else "not_available"
    })

if __name__ == '__main__':
    print("🚀 Starting AI Restaurant Assistant Server...")
    print("⏱️ Timeout settings: 30 seconds for API calls")
    app.run(host='0.0.0.0', port=5000, debug=True, threaded=True)
