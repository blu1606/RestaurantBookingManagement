"""
AI Agent Utilities

This package contains utility classes and helpers:
- RateLimiter: Rate limiting for API calls
"""

from .rate_limiter import RateLimiter

__all__ = ['RateLimiter']

def handle_pending_action_utils(user_input, tool, collected_params, missing_params, extract_params_fn, check_missing_fn, ask_for_missing_info_fn, create_response_fn):
    """
    Hàm dùng chung để xử lý bổ sung thông tin cho pending action (ask_for_info)
    """
    # Trích xuất params mới từ user_input
    new_params = extract_params_fn(user_input, tool)
    # Merge với params đã có
    merged_params = dict(collected_params)
    merged_params.update({k: v for k, v in new_params.items() if v is not None})
    # Kiểm tra còn thiếu gì không
    still_missing = check_missing_fn(merged_params, tool)
    if still_missing:
        # Hỏi tiếp
        ask_resp = ask_for_missing_info_fn(still_missing, tool, user_input)
        ask_resp["collected_params"] = merged_params
        ask_resp["missing_params"] = still_missing
        return ask_resp
    # Nếu đủ, thực hiện action
    return create_response_fn(
        action=tool["name"],
        parameters=merged_params,
        natural_response=f"Tôi sẽ thực hiện tác vụ: {tool['description']} với thông tin đã cung cấp."
    ) 