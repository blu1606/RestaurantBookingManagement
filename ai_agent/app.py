import os
import json
from flask import Flask, request, jsonify
from flask_cors import CORS
from collections import defaultdict

# Import the reorganized agents
from agents.core.agent_manager import AgentManager

# Load environment variables from .env if available
try:
    from dotenv import load_dotenv
    load_dotenv()
    print("✅ .env file loaded.")
except ImportError:
    print("⚠️ Warning: python-dotenv not installed. .env file will not be loaded.")

app = Flask(__name__)
CORS(app)

# Configure Gemini API
GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
gemini_model = None
if GOOGLE_API_KEY:
    try:
        import google.generativeai as genai
        genai.configure(api_key=GOOGLE_API_KEY)
        gemini_model = genai.GenerativeModel('gemini-2.5-flash')
        print("✅ Gemini API configured successfully.")
    except Exception as e:
        print(f"🔥 Error configuring Gemini API: {e}")
        gemini_model = None
else:
    print("⚠️ GOOGLE_API_KEY not found. Running in Fallback Mode.")

# Initialize AgentManager
agent_manager = None
if AgentManager:
    try:
        agent_manager = AgentManager(gemini_model)
        print("✅ AgentManager initialized successfully")
    except Exception as e:
        print(f"🔥 Error initializing AgentManager: {e}")
        agent_manager = None

@app.route('/process', methods=['POST'])
def process_user_input():
    """Main API endpoint to process user input using AgentManager."""
    try:
        data = request.get_json()
        if not data or 'userInput' not in data:
            return jsonify({"error": "Invalid input. 'userInput' field is required."}), 400

        user_input = data['userInput']
        session_id = data.get('sessionId', 'default')
        role = data.get('role', 'USER')
        
        print(f"➡️ Received input from session {session_id} (Role: {role}): {user_input}")

        if not agent_manager:
            return jsonify({
                "action": "error",
                "parameters": {},
                "naturalResponse": "Xin lỗi, hệ thống AI chưa sẵn sàng. Vui lòng thử lại sau.",
                "agent": "FallbackAgent",
                "routing": {
                    "intent": "fallback",
                    "agent": "FallbackAgent",
                    "confidence": 0.0
                }
            }), 500

        # Process through AgentManager
        response = agent_manager.process_user_input(user_input, session_id, role)
        
        print(f"✅ AgentManager response for session {session_id}: {response}")
        return jsonify(response)

    except Exception as e:
        print(f"🔥 Critical error in /process endpoint: {e}")
        return jsonify({
            "action": "error",
            "parameters": {},
            "naturalResponse": "Xin lỗi, có lỗi xảy ra. Vui lòng thử lại sau.",
            "agent": "FallbackAgent",
            "routing": {
                "intent": "fallback",
                "agent": "FallbackAgent",
                "confidence": 0.0
            }
        }), 500

@app.route('/refresh-knowledge', methods=['POST'])
def refresh_knowledge():
    """Endpoint để Java thông báo rằng dữ liệu đã thay đổi."""
    try:
        if agent_manager:
            agent_manager.refresh_all_knowledge()
            return jsonify({"status": "success", "message": "All agent knowledge bases refreshed."}), 200
        else:
            return jsonify({"status": "error", "message": "AgentManager not available."}), 500
    except Exception as e:
        print(f"🔥 Error in refresh-knowledge endpoint: {e}")
        return jsonify({"status": "error", "message": str(e)}), 500

@app.route('/health', methods=['GET'])
def health_check():
    """Health check endpoint."""
    agent_status = {}
    if agent_manager:
        agent_status = agent_manager.get_agent_status()
    
    return jsonify({
        "status": "healthy",
        "message": "AI Agent Manager is running",
        "gemini_api_status": "available" if gemini_model else "not_configured",
        "agent_manager_status": "available" if agent_manager else "not_available",
        "agent_status": agent_status
    })

@app.route('/agents/status', methods=['GET'])
def get_agents_status():
    """Get detailed status of all agents."""
    if not agent_manager:
        return jsonify({"error": "AgentManager not available"}), 500
    
    return jsonify({
        "agent_status": agent_manager.get_agent_status(),
        "total_agents": len(agent_manager.agents)
    })

@app.route('/conversation/history/<session_id>', methods=['GET'])
def get_conversation_history(session_id):
    """Get conversation history for a session."""
    if not agent_manager:
        return jsonify({"error": "AgentManager not available"}), 500
    
    history = agent_manager.get_conversation_history(session_id)
    return jsonify({
        "session_id": session_id,
        "history": history,
        "message_count": len(history)
    })

@app.route('/conversation/clear/<session_id>', methods=['POST'])
def clear_conversation_history(session_id):
    """Clear conversation history for a session."""
    if not agent_manager:
        return jsonify({"error": "AgentManager not available"}), 500
    
    agent_manager.clear_conversation_history(session_id)
    return jsonify({
        "status": "success",
        "message": f"Conversation history cleared for session {session_id}"
    })

if __name__ == '__main__':
    print("🚀 Starting AI Agent Manager Server...")
    print("📋 Available endpoints:")
    print("  - POST /process - Process user input")
    print("  - POST /refresh-knowledge - Refresh knowledge bases")
    print("  - GET /health - Health check")
    print("  - GET /agents/status - Agent status")
    print("  - GET /conversation/history/<session_id> - Get conversation history")
    print("  - POST /conversation/clear/<session_id> - Clear conversation history")
    
    if agent_manager:
        print("✅ AgentManager ready with specialized agents:")
        for agent_name in agent_manager.agents.keys():
            print(f"  - {agent_name}")
    else:
        print("⚠️ AgentManager not available")
    
    app.run(host='0.0.0.0', port=5000, debug=True, threaded=True) 