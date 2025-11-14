package com.example.helloworldapp.config

object AppConfig {

    // ============================================================
    //                       MAIN ACTIVITY CONFIG
    // ============================================================

    const val PREFS_NAME = "UserPrefs"

    // SharedPreferences Keys
    const val KEY_NAME = "name"
    const val KEY_ROLE = "role"
    const val KEY_EMAIL = "email"
    const val KEY_USER_ID = "userId"
    const val KEY_DEVICE_ID = "deviceId"
    const val KEY_PROTECTION = "protection"

    // Login-specific keys
    const val KEY_SESSION_ID = "session_id"
    const val KEY_TRANSACTION_CAM1 = "transaction_id_cam1"
    const val KEY_TRANSACTION_CAM2 = "transaction_id_cam2"

    const val DEFAULT_PROTECTION = "disabled"

    const val WIFI_ERROR_MESSAGE = "Please connect to a Wi-Fi network to continue."


    // ============================================================
    //                     REGISTRATION PAGE CONFIG
    // ============================================================

    const val BACKEND_URL = "http://192.168.1.7:9000"

    const val ENDPOINT_REGISTER = "/register"
    const val ENDPOINT_VERIFY_OTP = "/verify_otp"
    const val ENDPOINT_HELLO = "/hello"

    const val COMPANY_NAME = "JLMILLS"
    const val BRANCH = "Rajapalayam"
    const val SUB_BRANCH = "None"

    val ROLES = listOf("MD", "JMD", "GM", "AGM", "IT HEAD", "SUPERVISOR")

    val PASSWORD_REGEX =
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$".toRegex()
    val EMAIL_REGEX =
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".toRegex()


    // ============================================================
    //                       LOGIN ACTIVITY CONFIG
    // ============================================================

    const val LOGIN_BACKEND_URL = "http://192.168.1.7:9010"
    const val ENDPOINT_LOGIN = "/api/auth/login"

    const val TOAST_REUSE_SESSION = "Reusing existing session (active detection running)"
    const val TOAST_SESSION_FAILED = "Failed to generate session"



    // ============================================================
    //                 PROTECTION SETTINGS CONFIG
    // ============================================================

    const val PROTECTION_ENABLED = "enabled"

    const val TITLE_PROTECTION_SETTINGS = "Device Protection Settings"
    const val BUTTON_ENABLE_PROTECTION = "Enable Protection"
    const val BUTTON_DISABLE_PROTECTION = "Disable Protection"

    const val ERROR_SET_DEVICE_SECURITY =
        "Please set a device PIN, pattern, or password first!"
    const val SUCCESS_PROTECTION_ENABLED = "Protection enabled successfully!"
    const val PROTECTION_DISABLED = "Protection disabled!"
    const val ERROR_NO_BIOMETRIC =
        "Your device doesn’t support biometrics or PIN protection."
    const val ERROR_NONE_ENROLLED =
        "No biometric or PIN registered. Please set up device security in Settings."


    // ============================================================
    //                    CAMERA SELECTION CONFIG
    // ============================================================

    const val TITLE_CAMERA_SELECTION = "🎥 Camera Selection"
    const val BUTTON_CAM1 = "Cam 1"
    const val BUTTON_CAM2 = "Cam 2"

    const val MENU_TITLE = "📋 Menu"
    const val MENU_DASHBOARD = "🏠 Dashboard"

    const val DIALOG_USER_INFO_TITLE = "User Info"
    const val BUTTON_CLOSE = "Close"


    // ============================================================
    //                     CAM 1 PAGE CONFIG
    // ============================================================

    // API Endpoints
    const val CAM1_SERVER_IP = "192.168.1.7"
    const val CAM1_START_URL = "http://192.168.1.7:8000/start"
    const val CAM1_STOP_URL = "http://192.168.1.7:8000/stop"

    // Labels & UI
    const val CAM1_TITLE = "Detection Control - Cam 1"
    const val CAM1_SUPERVISOR_LABEL = "Supervisor"
    const val CAM1_SESSION_LABEL = "Session ID"
    const val CAM1_TRANSACTION_LABEL = "Transaction ID"
    const val CAM1_VEHICLE_LABEL = "Vehicle Number"

    const val CAM1_BUTTON_START = "Start"
    const val CAM1_BUTTON_STOP = "Stop"

    const val CAM1_INVALID_TITLE = "Invalid Vehicle Format"
    const val CAM1_INVALID_MESSAGE = "This number looks incorrect. Continue anyway?"
    const val CAM1_INVALID_YES = "Yes"
    const val CAM1_INVALID_CANCEL = "Cancel"

    // Toasts
    const val CAM1_TOAST_NO_SESSION = "No session ID!"
    const val CAM1_TOAST_STARTED = "Detection Started!"
    const val CAM1_TOAST_STOPPED = "Detection Stopped!"
    const val CAM1_TOAST_DETECTION_STOPPED = "Detection stopped"



// ============================================================
//                     CAM 2 PAGE CONFIG
// ============================================================

    // API Endpoints
    const val CAM2_SERVER_IP = "192.168.1.7"
    const val CAM2_START_URL = "http://192.168.1.7:8001/start"
    const val CAM2_STOP_URL = "http://192.168.1.7:8001/stop"

    // Labels & UI
    const val CAM2_TITLE = "Detection Control - Cam 2"
    const val CAM2_SUPERVISOR_LABEL = "Supervisor"
    const val CAM2_SESSION_LABEL = "Session ID"
    const val CAM2_TRANSACTION_LABEL = "Transaction ID"
    const val CAM2_VEHICLE_LABEL = "Vehicle Number"

    const val CAM2_BUTTON_START = "Start"
    const val CAM2_BUTTON_STOP = "Stop"

    const val CAM2_INVALID_TITLE = "Invalid Vehicle Format"
    const val CAM2_INVALID_MESSAGE = "This number looks incorrect. Continue anyway?"
    const val CAM2_INVALID_YES = "Yes"
    const val CAM2_INVALID_CANCEL = "Cancel"

    // Toasts
    const val CAM2_TOAST_NO_SESSION = "No session ID!"
    const val CAM2_TOAST_STARTED = "CAM2 Detection Started!"
    const val CAM2_TOAST_STOPPED = "CAM2 Detection Stopped!"
    const val CAM2_TOAST_DETECTION_STOPPED = "CAM2 Detection stopped"

    // ============================================================
//                     DASHBOARD CONFIG
// ============================================================

    const val DASHBOARD_API_BASE_URL = "http://192.168.1.7:9020/"

    const val ENDPOINT_TODAY_TRANSACTIONS = "api/transactions/today"
    const val ENDPOINT_GROUPED_TRANSACTIONS = "api/transactions/grouped"

    // Titles
    const val DASHBOARD_TITLE = "📊 7-Day Loading Dashboard"
    const val TODAY_SECTION_TITLE = "📅 Today’s Active Loadings"
    const val LAST_7_DAYS_TITLE = "🗓 Last 7 Days Loadings"

    // Labels & Badges
    const val LABEL_TODAY = "Today"
    const val STATUS_LIVE = "🟢 LIVE"
    const val STATUS_DONE = "✅ DONE"
    const val NO_IMAGE_AVAILABLE = "❌ No image available"

    // Counts (CAM1 & CAM2)
    const val LABEL_BOX = "📦"
    const val LABEL_BALE = "🧵"
    const val LABEL_TROLLEY = "🛒"
    const val LABEL_BAG = "🎒"

    // Messages
    const val DASHBOARD_LOADING_MESSAGE = "⏳ Fetching loadings..."
    const val REFRESH_CONTENT_DESCRIPTION = "Refresh"

    // Animation
    const val CARD_ANIMATION_DURATION = 300

}