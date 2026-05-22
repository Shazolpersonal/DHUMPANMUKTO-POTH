package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkText
import com.example.ui.theme.LightBg
import com.example.ui.theme.CardWhite
import com.example.ui.theme.AlertRed
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.TealDark
import com.example.ui.theme.MutedText
import com.example.ui.theme.BorderGray
import com.example.ui.theme.SoftAmber
import com.example.ui.theme.AlertRedLight
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

// ==========================================
// DATA MODELS
// ==========================================

data class Situation(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: String,
    val actions: List<SOSAction>
)

data class SOSAction(
    val id: Int,
    val text: String
)

data class HarmfulEffect(
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class QuitReason(
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class BodyMilestone(
    val id: Int,
    val durationText: String,
    val durationMillis: Long,
    val title: String,
    val description: String
)

data class Tip(
    val title: String,
    val description: String,
    val category: String, // "physical", "mental", "spiritual", "social"
    val icon: ImageVector
)

// ==========================================
// VIEWMODEL FOR STATE MANAGEMENT
// ==========================================

class AppViewModel(private val sharedPrefs: SharedPreferences) : ViewModel() {
    var quitTimestamp by mutableStateOf(sharedPrefs.getLong("quit_timestamp", 0L))
        private set
    var dailyCigaretteCount by mutableStateOf(sharedPrefs.getInt("daily_count", 10))
        private set
    var notificationEnabled by mutableStateOf(sharedPrefs.getBoolean("notif_enabled", true))
        private set
    var completedActions by mutableStateOf(
        sharedPrefs.getStringSet("completed_actions", emptySet())
            ?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    )
        private set

    fun setQuitTime(timestamp: Long) {
        quitTimestamp = timestamp
        sharedPrefs.edit().putLong("quit_timestamp", timestamp).apply()
    }

    fun setCigaretteCount(count: Int) {
        dailyCigaretteCount = count
        sharedPrefs.edit().putInt("daily_count", count).apply()
    }

    fun setNotifications(enabled: Boolean) {
        notificationEnabled = enabled
        sharedPrefs.edit().putBoolean("notif_enabled", enabled).apply()
    }

    fun toggleActionCompleted(actionId: Int) {
        val newSet = if (completedActions.contains(actionId)) {
            completedActions - actionId
        } else {
            completedActions + actionId
        }
        completedActions = newSet
        sharedPrefs.edit().putStringSet(
            "completed_actions",
            newSet.map { it.toString() }.toSet()
        ).apply()
    }

    fun resetApp() {
        quitTimestamp = 0L
        dailyCigaretteCount = 10
        notificationEnabled = true
        completedActions = emptySet()
        sharedPrefs.edit().clear().apply()
    }
}

// ==========================================
// MAIN COMPONENT ACTIVITY
// ==========================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val sharedPrefs = getSharedPreferences("SmokeFreePrefs", MODE_PRIVATE)
        val viewModel = AppViewModel(sharedPrefs)

        setContent {
            MyApplicationTheme {
                MainAppScreen(viewModel)
            }
        }
    }
}

// Helper to convert numbers to Bengali characters for visual authenticity
fun toBengaliNum(input: String): String {
    val engToBen = mapOf(
        '0' to '০', '1' to '১', '2' to '২', '3' to '৩', '4' to '৪',
        '5' to '৫', '6' to '৬', '7' to '৭', '8' to '৮', '9' to '৯'
    )
    return input.map { engToBen[it] ?: it }.joinToString("")
}

fun toBengaliNumId(id: Int): String {
    return toBengaliNum(id.toString())
}

// ==========================================
// SOS ACTIONS MAPPED DATA
// ==========================================

val SOS_DATA = listOf(
    Situation(
        id = 1,
        title = "ঘুম থেকে ওঠার পর",
        subtitle = "Morning Craving",
        icon = "🌅",
        actions = listOf(
            SOSAction(1, "১ মিনিটের ব্রাশ: বিছানা ছাড়ার ১ মিনিটের মধ্যে কড়া মিন্ট পেস্ট দিয়ে দাঁত মাজুন; মুখের ফ্রেশ ভাব নিকোটিনের ইচ্ছা কমিয়ে দেয়।"),
            SOSAction(2, "ওযু বা ঠান্ডা পানি: চোখে-মুখে ঠান্ডা পানির ঝাপটা দিন বা ওযু করে সকালের অলসতা কাটান।"),
            SOSAction(3, "৩ চুমুকে পানি: বসে শান্ত হয়ে এক গ্লাস ঠান্ডা পানি ৩ চুমুকে ধীরলয়ে পান করুন।"),
            SOSAction(4, "দ্রুত বিছানা ত্যাগ: ঘুম ভাঙার পর বিছানায় অলসভাবে শুয়ে ফোন স্ক্রোল করবেন না; দ্রুত উঠে পড়ুন।"),
            SOSAction(5, "৫টি গভীর শ্বাস: খোলা জানালায় বা বারান্দায় গিয়ে বুক ভরে ৫ বার লম্বা শ্বাস নিন ও ছাড়ুন।"),
            SOSAction(6, "রুটিন পরিবর্তন: সকালে আগে ভরপেট নাস্তা করুন, তারপর চা বা কফি পানের টেবিলে যান।")
        )
    ),
    Situation(
        id = 2,
        title = "প্রধান খাবার খাওয়ার পর",
        subtitle = "After Meals",
        icon = "🍽️",
        actions = listOf(
            SOSAction(7, "দ্রুত আসন ত্যাগ: শেষ লোকমা মুখে দিয়েই ডাইনিং টেবিল বা চেয়ার ছেড়ে উঠে পড়ুন।"),
            SOSAction(8, "মাউথওয়াশ বা কুলকুচি: খাওয়ার পরপরই ভালো করে কুলকুচি বা সুগন্ধি মাউথওয়াশ ব্যবহার করুন।"),
            SOSAction(9, "প্রাকৃতিক ফ্রেশনার: ডাইনিং টেবিলে লবঙ্গ বা এলাচ রাখুন; খাওয়া শেষে একটি মুখে দিয়ে চুষতে থাকুন।"),
            SOSAction(10, "৫ মিনিটের পায়চারি: খাওয়ার পর অলসভাবে বসে না থেকে ঘরে বা বারান্দায় ৫ মিনিট হাঁটুন।"),
            SOSAction(11, "ওযু বা নামাজ: দুপুরের বা রাতের খাবারের পর ওযু করে ফরজ বা নফল নামাজে দাঁড়িয়ে যান।"),
            SOSAction(12, "হাত ব্যস্ত করা: খাওয়ার পর টেবিল পরিষ্কার করা বা থালাবাসন ধোয়ায় পরিবারকে সাহায্য করুন।")
        )
    ),
    Situation(
        id = 3,
        title = "কর্মক্ষেত্রে কাজের মাঝে",
        subtitle = "Work Breaks",
        icon = "💻",
        actions = listOf(
            SOSAction(13, "ডেস্কেই অবস্থান: ধূমপায়ী সহকর্মীরা বিরতিতে গেলে আপনি নিজের ডেস্কে বসে কাজ গুছিয়ে নিন।"),
            SOSAction(14, "সুস্থ পানীয়: কড়া ধোঁয়াটে চায়ের বদলে ডাবের পানি, লেবু পানি বা গ্রিন টি পানের অভ্যাস করুন।"),
            SOSAction(15, "হাতের খেলনা: হাত ব্যস্ত রাখতে ডেস্কে একটি স্ট্রেস বল (Stress Ball) বা তাসবিহ কাউন্টার রাখুন।"),
            SOSAction(16, "ইস্তিগফার পাঠ: কাজের ক্লান্তিতে মনে মনে 'আস্তাগফিরুল্লাহ' পড়ে মানসিক শক্তি ও মনোযোগ বাড়ান।"),
            SOSAction(17, "করিডোরে হাঁটা: ক্লান্তি আসলে ধূমপানের জায়গায় না গিয়ে অফিসের অন্য করিডোর বা সিঁড়িতে হাঁটুন।"),
            SOSAction(18, "২ মিনিটের স্ক্রিন ব্রেক: ডেস্কে বসে কোনো শিক্ষণীয় বা অনুপ্রেরণামূলক ছোট ভিডিও দেখুন।")
        )
    ),
    Situation(
        id = 4,
        title = "বন্ধুদের সাথে আড্ডায়",
        subtitle = "Social Settings",
        icon = "👥",
        actions = listOf(
            SOSAction(19, "স্পষ্ট ঘোষণা: আড্ডার শুরুতেই হাসিমুখে দৃঢ়ভাবে বলুন, \"আমি ধূমপান ছেড়ে দিয়েছি, কেউ সাধবেন না।\""),
            SOSAction(20, "অধূমপায়ীর পাশে বসা: আড্ডায় সবসময় অধূমপায়ী বা ভালো স্বভাবের বন্ধুর পাশে আসন নিন।"),
            SOSAction(21, "হাতে গ্লাস রাখা: নিজের হাত ব্যস্ত রাখতে পানির গ্লাস বা জুসের মগ শক্ত করে ধরে রাখুন।"),
            SOSAction(22, "ওযুর অজুহাত: সবাই একসাথে সিগারেট জ্বালালে আপনি ওযু বা ওয়াশরুমের বাহানায় দূরে যান।"),
            SOSAction(23, "নো-স্মোকিং জোন: রেস্তোরাঁয় বা পার্টিতে গেলে কঠোরভাবে \"No Smoking\" বা এসি জোনে বসুন।"),
            SOSAction(24, "আড্ডা সংক্ষিপ্ত করা: অলস বা দীর্ঘ আড্ডা এড়িয়ে মূল কথা শেষে সালাম দিয়ে বিদায় নিন।")
        )
    ),
    Situation(
        id = 5,
        title = "মানসিক চাপ বা রাগের সময়",
        subtitle = "Stress & Anxiety",
        icon = "⚡",
        actions = listOf(
            SOSAction(25, "আউযুবিল্লাহ পাঠ: রাগ বা চাপ অনুভব করলেই \"আউযুবিল্লাহি মিনাশ শাইতানির রাজিম\" পড়ুন।"),
            SOSAction(26, "বসার ভঙ্গি বদল: দাঁড়িয়ে থাকলে বসে পড়ুন, আর বসে থাকলে শুয়ে পড়ুন (সুন্নাত পদ্ধতি)।"),
            SOSAction(27, "তাৎক্ষণিক ওযু: দ্রুত ঠান্ডা পানি দিয়ে ওযু করুন, এটি রাগ ও স্নায়বিক উত্তেজনা তাৎক্ষণিক কমায়।"),
            SOSAction(28, "৪-৭-৮ শ্বাস: ৪ সেকেন্ড নাক দিয়ে শ্বাস নিন, ৭ সেকেন্ড ধরে রাখুন, ৮ সেকেন্ড ধরে মুখ দিয়ে ছাড়ুন।"),
            SOSAction(29, "স্থান পরিবর্তন: যে রুমে বা পরিস্থিতিতে রাগ উঠবে, দ্রুত সেখান থেকে খোলা বাতাসে বেরিয়ে যান।"),
            SOSAction(30, "প্রিয়জনকে কল: মন খারাপ বা অস্থির লাগলে বিশ্বস্ত কোনো দ্বীনি বন্ধু বা পরিবারকে কল করে কথা বলুন।")
        )
    ),
    Situation(
        id = 6,
        title = "একা থাকলে বা একঘেয়েমি লাগলে",
        subtitle = "Boredom",
        icon = "⛺",
        actions = listOf(
            SOSAction(31, "তিলাওয়াত শ্রবণ: একা ঘরে মন ছটফট করলে সুন্দর কণ্ঠে কুরআন তিলাওয়াত শুনুন।"),
            SOSAction(32, "খোসাওয়ালা বাদাম: এক বাটি চিনা বাদাম একটা একটা করে ছুলে খান (হাত ও মুখ ব্যস্ত থাকবে)।"),
            SOSAction(33, "ঘর বা টেবিল গোছানো: অলস সময়ে নিজের আলমারি, ড্রয়ার বা টেবিল গোছাতে লেগে যান।"),
            SOSAction(34, "জ্ঞানমূলক ভিডিও: একঘেয়েমি কাটাতে তথ্যচিত্র বা শিক্ষণীয় ইসলামিক লেকচার দেখুন।"),
            SOSAction(35, "বুদ্ধির খেলা: ফোনে কোনো জটিল সুডোকু, দাবা বা পাজল গেম খেলুন।"),
            SOSAction(36, "ট্র্যাকার অ্যাপ চেক: ধূমপান না করায় কত টাকা ও আয়ু বাঁচলো তা অ্যাপে দেখে মোটিভেশন নিন।")
        )
    ),
    Situation(
        id = 7,
        title = "যাতায়াত ও জ্যামের সময়",
        subtitle = "Commuting",
        icon = "🚗",
        actions = listOf(
            SOSAction(37, "পকেটে ক্যান্ডি/আদা: বাইরে বের হওয়ার সময় পকেটে সবসময় কড়া মেন্টস বা আদা কুচি রাখুন।"),
            SOSAction(38, "অডিওবুক বা পডকাস্ট: যাতায়াতের সময় কানে হেডফোন দিয়ে কোনো পডকাস্ট বা অডিওবুক শুনুন।"),
            SOSAction(39, "চেনা পথ বর্জন: যে মোড়ের দোকান থেকে নিয়মিত cigarette কিনতেন, সেই পথটি এড়িয়ে চলুন।"),
            SOSAction(40, "গাড়ি পরিষ্কার রাখা: ব্যক্তিগত গাড়ি বা বাইক থেকে লাইটার, ম্যাচ বা অ্যাশট্রে চিরতরে ফেলে দিন।"),
            SOSAction(41, "টুথপিক বা স্ট্র চিবানো: ড্রাইভিং বা জ্যামে ঠোঁটের শূন্যতা দূর করতে একটি পরিষ্কার টুথপিক মুখে রাখুন।"),
            SOSAction(42, "জিকিরে জবান মশগুল: যাতায়াতের পুরো সময় সুবহানাল্লাহ, আলহামদুলিল্লাহ জিকির করতে থাকুন।")
        )
    ),
    Situation(
        id = 8,
        title = "হঠাৎ তীব্র ক্রেভিং বা ইচ্ছা হলে",
        subtitle = "Sudden Craving",
        icon = "🔥",
        actions = listOf(
            SOSAction(43, "৫ মিনিটের চ্যালেঞ্জ: ঘড়ির দিকে তাকিয়ে ৫ মিনিট অপেক্ষা করুন; ক্রেভিং বা তীব্র ইচ্ছা নিজে থেকেই কমে যাবে।"),
            SOSAction(44, "উল্টো গণনা: মনে মনে ১০০ থেকে ১ পর্যন্ত উল্টো দিকে দ্রুত গণনা করুন (মস্তিষ্ক ডাইভার্ট হবে)।"),
            SOSAction(45, "বরফ থেরাপি: মুখে এক টুকরো বরফ নিয়ে চুষতে থাকুন, এটি তাৎক্ষণিক মনোযোগ ঘুরিয়ে দেবে।"),
            SOSAction(46, "পুশ-আপ দেওয়া: তীব্র ইচ্ছা হওয়া মাত্রই মাটিতে বা দেওয়ালে ১০-২০টি বুকডন (Push-ups) দিন।"),
            SOSAction(47, "দোয়ায় হাত তোলা: সরাসরি আল্লাহর কাছে সাহায্য চান: \"ইয়া আল্লাহ, এই নেশা থেকে আমাকে বাঁচান।\""),
            SOSAction(48, "লক্ষ্যের ছবি দেখা: ফোনের হোমস্ক্রিনে আপনার সন্তান বা পরিবারের ছবি রাখুন ও তীব্র ইচ্ছার সময় তা দেখুন।")
        )
    ),
    Situation(
        id = 9,
        title = "ঘর ও পরিবেশ নিয়ন্ত্রণে",
        subtitle = "Environment Control",
        icon = "🏡",
        actions = listOf(
            SOSAction(49, "গন্ধ দূর করা: ঘরের যেসব পর্দা, চাদর ও পোশাকের কাপড়ে গন্ধ লেগে আছে, দ্রুত ধুয়ে ফেলুন।"),
            SOSAction(50, "সুগন্ধি ব্যবহার: ঘর ও গাড়িতে নিয়মিত আতর, এয়ার ফ্রেশনার বা সুগন্ধি ব্যবহার করুন।"),
            SOSAction(51, "সদকার কাঁচের পাত্র: সিগারেটের বাঁচানো টাকা পাত্রে রাখুন এবং মাস শেষে এতিমখানায় দান করুন।"),
            SOSAction(52, "শেষ উপাদান বর্জন: ড্রয়ারে লুকিয়ে রাখা শেষ সিগারেট বা খালি প্যাকেটটিও আজই ফেলে দিন।"),
            SOSAction(53, "নো-স্মোকিং সাইন: নিজের পড়ার টেবিল বা ঘরের দরজায় ছোট \"No Smoking\" স্টিকার লাগান।"),
            SOSAction(54, "ধোঁয়াটে আড্ডা বর্জন: যেখানে তামাক বিক্রি বা ধূমপান হয়, সেসব জায়গা কঠোরভাবে এড়িয়ে চলুন।")
        )
    ),
    Situation(
        id = 10,
        title = "দীর্ঘমেয়াদী সফলতার জন্য",
        subtitle = "Long-term Strategy",
        icon = "🎯",
        actions = listOf(
            SOSAction(55, "\"আজকের দিনটি\" নীতি: সারাজীবন ছাড়ার ভয় না পেয়ে বলুন, \"আমি শুধু আজ ধূমপান করব না।\""),
            SOSAction(56, "রোজা রাখা (সিয়াম): আত্মনিয়ন্ত্রণ ক্ষমতা ও তাকওয়া বাড়াতে প্রতি সোম ও বৃহস্পতিবার নফল রোজা রাখুন।"),
            SOSAction(57, "তওবা ও নতুন শুরু: ভুলবশত একদিন খেয়ে ফেললে নিরাশ না হয়ে তওবা করে আবার শুরু করুন।"),
            SOSAction(58, "নিজেকে হালাল পুরস্কার: ধূমপান ছাড়া ১ম সপ্তাহ বা মাস পূর্ণ হলে নিজেকে ভালো বই বা খাবার উপহার দিন।"),
            SOSAction(59, "নিকোটিন রিপ্লেสมেন্ট: ক্রেভিং অতিরিক্ত ও অসহ্য হলে সাময়িকভাবে ফার্মেসি থেকে নিকোটিন গাম কিনে চিবান।"),
            SOSAction(60, "চিকিৎসকের পরামর্শ: তীব্র অনিদ্রা বা শারীরিক সমস্যা হলে জাতীয় বক্ষব্যাধি ইনস্টিটিউট ও হাসপাতাল-এর বিশেষজ্ঞ চিকিৎসকের পরামর্শ নিন।")
        )
    )
)

// ==========================================
// HEALTH AND TIMELINE DATA
// ==========================================

val HARMFUL_EFFECTS = listOf(
    HarmfulEffect("ফুসফুস (Lungs)", "ফুসফুসে আলকাতরা ও ক্ষতিকর নিকোটিন জমার কারণে ব্রঙ্কাইটিস, হাঁপানি এবং শেষমেশ ফুসফুসের ভয়াণক ক্যান্সার সৃষ্টি হতে পারে।", Icons.Default.Air),
    HarmfulEffect("হৃৎপিণ্ড (Heart)", "ধূমপানে ধমনী শক্ত হয়ে রক্ত সঞ্চালনে বাধা সৃষ্টি হয়, যা রক্তচাপ অনিয়ন্ত্রিত করে এবং স্ট্রোক ও হার্ট অ্যাটাকের ঝুঁকি বহুগুণ বাড়িয়ে দেয়।", Icons.Default.Favorite),
    HarmfulEffect("মস্তিষ্ক (Brain)", "মস্তিষ্কের রক্তনালী মারাত্মকভাবে ক্ষতিগ্রস্ত হয়ে তীব্র স্ট্রোক (Brain Stroke) ও স্মৃতিশক্তির দীর্ঘস্থায়ী দুর্বলতা ঘটতে পারে।", Icons.Default.Psychology),
    HarmfulEffect("ত্বক ও চেহারা (Skin)", "ত্বকের কোলাজেন চিরতরে নষ্ট হয়ে অল্প বয়সে রিঙ্কেলস পড়ে এবং ফ্যাকাশে, নিস্তেজ ও বুড়িয়ে যাওয়া চেহারা ধারণ করে।", Icons.Default.Face),
    HarmfulEffect("দাঁত ও মাড়ি (Teeth)", "দাঁত ও মাড়িতে কুৎসিত হলদে-কালো দাগ পড়ে যায়, মাড়ি থেকে রক্ত নির্গত হয় এবং মুখে তীব্র সামাজিক দুর্গন্ধ তৈরি করে।", Icons.Default.SentimentSatisfied),
    HarmfulEffect("প্রজনন ক্ষমতা (Fertility)", "নারী-পুরুষ উভয়ের হরমোনাল ভারসাম্য নষ্ট করে এবং সন্তান ধারণের স্বাভাবিক প্রজনন ক্ষমতা মারাত্মকভাবে হ্রাস করে।", Icons.Default.SupervisorAccount),
    HarmfulEffect("পরিবার ও শিশু (Family Support)", "ধোঁয়ার কারণে নিজের অসচেতনতায় সন্তান ও পরিবারের মূল্যবান সদস্যদের ফুসফুসে পরোক্ষ ধূমপানের বিষ প্রবেশ করে জটিল রোগ সৃষ্টি করে।", Icons.Default.People)
)

val QUIT_REASONS = listOf(
    QuitReason("শারীরিক সুস্থতা (Health)", "বিষাক্ত ৪০০০+ রাসায়নিক বর্জনে আপনার ফুসফুস পুনরায় সতেজ হবে ও বার্ধক্যেও পাবেন তারুণ্যের প্রাণোচ্ছল কর্মশক্তি।", Icons.Default.HealthAndSafety),
    QuitReason("পরিবারের মুখে হাসি (Family)", "আপনার সুস্বাস্থ্যই আপনার পরিবারের সর্বশ্রেষ্ঠ আশীর্বাদ; ক্ষতিকর পরোক্ষ ধূমপান থেকে তারা পাবে শতভাগ নিরাপদ আশ্রয়।", Icons.Default.FamilyRestroom),
    QuitReason("দ্বীনি পবিত্রতা (Faith / Islam)", "ধূমপান ইসলাম শরিয়তে অত্যন্ত ক্ষতিকর বিধায় অপছন্দনীয় বা নিষিদ্ধ। পবিত্র মন ও পরিচ্ছন্ন শরীর নিয়ে আল্লাহর সান্নিধ্য লাভ অনেক সহজ হয়।", Icons.Default.Mosque),
    QuitReason("আর্থিক প্রাচুর্য ও সদকা (Savings)", "সিগারেটে অকারণে অর্থ পুড়িয়ে অপচয় বন্ধ করে সেই অর্থ অভাবী এতিমদের কল্যাণে সদকা দান করে পরকালের সওয়াব অর্জন করুন।", Icons.Default.Savings),
    QuitReason("আত্মমর্যাদা ও ব্যক্তিত্ব (Self-Respect)", "তামাক ও নিকোটিন আসক্তির ক্ষুদ্র দাসত্ব ভেঙে নিজের দৃঢ় মন ও সংযমের শ্রেষ্ঠ পরিচয় দিয়ে সমাজে নিজের আত্মমর্যাদা পুনরুদ্ধার করুন।", Icons.Default.Shield),
    QuitReason("অমূল্য দীর্ঘ আয়ু (Longevity)", "প্রতিটি তামাকের ধোঁয়া বর্জন করে পরম করুণাময়ের দেওয়া সুন্দর জীবনকে নিরোগ রেখে বেশিদিন বেঁচে থাকার এক স্বাস্থ্যকর আত্মরক্ষা।", Icons.Default.HourglassEmpty)
)

val HEALTH_MILESTONES = listOf(
    BodyMilestone(1, "২০ মিনিট", 20 * 60 * 1000L, "রক্তচাপ ও পালস হ্রাস", "রক্তচাপ এবং পালস রেট পুনরায় দ্রুত স্বাভাবিক অবস্থায় ফিরে আসতে শুরু করে।"),
    BodyMilestone(2, "৮ ঘণ্টা", 8 * 3600 * 1000L, "কার্বন মনোক্সাইড হ্রস্বতা", "রক্তে বিষাক্ত কার্বন মনোক্সাইডের মাত্রা অর্ধেক কমে অক্সিজেন স্তর স্বাস্থ্যকর মাত্রায় ফেরে।"),
    BodyMilestone(3, "২৪ ঘণ্টা", 24 * 3600 * 1000L, "হার্ট অ্যাটাকের ঝুঁকি হ্রাস", "ফুসফুস জমে থাকা বিষাক্ত কফ সরাতে শুরু করে এবং হার্ট অ্যাটাকের দীর্ঘ ঝুঁকি হ্রাস পেতে শুরু হয়।"),
    BodyMilestone(4, "৪৮ ঘণ্টা", 48 * 3600 * 1000L, "স্বাদ ও নাকের ঘ্রাণ পুনরুদ্ধার", "নিকোটিন শরীর থেকে সম্পূর্ণ মুক্ত হয়। স্বাদ এবং ঘ্রাণ নেওয়ার সূক্ষ্ম অনুভূতি অনেক সতেজ হয়ে ওঠে।"),
    BodyMilestone(5, "৭২ ঘণ্টা", 72 * 3600 * 1000L, "সহজ গভীর শ্বাস-প্রশ্বাস", "শ্বাসনালীগুলো সংকোচন কাটিয়ে শিথিল হয়, ফলে শ্বাস নেওয়া অনেক হালকা এবং আরামদায়ক বোধ হয়।"),
    BodyMilestone(6, "১ সপ্তাহ", 7 * 24 * 3600 * 1000L, "পিক ক্রেভিং কাটিয়ে ওঠা", "নিকোটিনের প্রত্যাহারজনিত মানসিক অস্থিরতা বা ক্রেভিং তীব্রতা অনেক কমে আসে।"),
    BodyMilestone(7, "১ মাস", 30 * 24 * 3600 * 1000L, "কাশি ও কফ স্থায়ী সমাধান", "ফুসফুসের সিলিয়া বা শ্বাসতন্ত্রের প্রতিরোধ ব্যবস্থা সতেজ হতে শুরু হওয়ায় দীর্ঘস্থায়ী কাশি ও কোলাহল কমে।"),
    BodyMilestone(8, "৩ মাস", 90 * 24 * 3600 * 1000L, "রক্ত সঞ্চালন ও স্ট্যামিনা বৃদ্ধি", "সার্বিক রক্ত সঞ্চালন অনেক গুণ সুসংহত হয় এবং সামান্য পরিশ্রমেই ক্লান্তি আসা বন্ধ হয়ে যায়।"),
    BodyMilestone(9, "১ বছর", 365 * 24 * 3600 * 1000L, "হৃদরোগের ঝুঁকি ৫০% পতন", "করোনারি হৃদরোগ হওয়ার অতিরিক্ত ঝুঁকি একজন অধূমপায়ীর চেয়ে অর্ধেক স্তরে নেমে যায়।"),
    BodyMilestone(10, "১০ বছর", 10 * 365 * 24 * 3600 * 1000L, "ফুসফুসের ক্যান্সার মুক্তি", "ফুসফুসের ক্যান্সারের ফলে মৃত্যুর ঝুঁকি সাধারণ ধূমপায়ীর তুলনায় প্রায় অর্ধেক হ্রাস পায়।")
)

val TIPS_DATA = listOf(
    Tip("নিয়মিত ১০ মিনিট ব্যায়াম", "সকালে ঘুম থেকে উঠে কিছুটা সময় জোরে হাঁটুন বা বুকডন দিন। এটি ঘামের মাধ্যমে শরীর থেকে ক্ষতিকর নিকোটিন বের করতে ও মনকে সতেজ করতে দারুণ উপকারী।", "physical", Icons.Default.DirectionsRun),
    Tip("হালকা গরম জল ও লেবু", "সকালে খালি পেটে কুসুম গরম পানিতে লেবু চিপে পান করুন। এটি লিভার পরিষ্কার করে ও ধূমপান করার আকস্মিক ইচ্ছা প্রশমিত করে।", "physical", Icons.Default.LocalCafe),
    Tip("৪ সেকেন্ডের ধ্যান ও বিরতি", "যখনই ধূমপানের জন্য মন আনচান করবে, তখনই চোখ বন্ধ করে পরম শান্তিতে ৪ সেকেন্ড বুক ভরে শ্বাস নিন ও ছাড়ুন। নিজেকে শান্ত রাখুন।", "mental", Icons.Default.Spa),
    Tip("জার্নাল বা অনুভূতি ডায়েরি", "আপনার ধূমপানবিহীন সাফল্যের দিনগুলোর অনুভূতি এবং কেন আপনি ধূমপানমুক্ত থাকতে চান তা ডায়েরিতে লিখে রাখুন। এটি দারুণ প্রেরণা জোগাবে।", "mental", Icons.Default.NoteAlt),
    Tip("নফল নামাজ ও ইস্তিগফার", "মন যখনই প্ররোচিত হবে তখন তাৎক্ষণিক ওযু করে ২ রাকাত নফল সালাত আদায় করুন অথবা মনে মনে 'আস্তাগফিরুল্লাহ' জিকির করতে থাকুন। শয়তানি শক্তি দূর হবে।", "spiritual", Icons.Default.Mosque),
    Tip("সকালের দোয়া ও আশ্রয় প্রার্থনা", "সকালে বাসা থেকে বের হওয়ার সময় আল্লাহর কাছে শয়তানের অনিষ্টতা এবং তামাকের অনিষ্টতা থেকে বাঁচার জন্য খাটি নিয়তে দোয়া ও রহমত কামনা করুন।", "spiritual", Icons.Default.MenuBook),
    Tip("অধূমপায়ী বন্ধুবলয় তৈরি", "আড্ডার জন্য এমন বন্ধুদের বেছে নিন যারা মুখে সিগারেট নেয় না এবং স্বাস্থ্য সচেতন জীবনকে ভালোবাসে। ধোঁয়াটে আড্ডা এড়িয়ে চলুন।", "social", Icons.Default.Group),
    Tip("সাফল্য ভাগাভাগি ও মিষ্টিমুখ", "ধূমপানমুক্ত ১ সপ্তাহ বা ১ মাস পেরোলে পরিবারকে নিয়ে সুন্দর মিষ্টিমুখ বা রেস্টুরেন্টে ডিনার উপভোগ করে নিজের চেষ্টা পুরস্কৃত করুন।", "social", Icons.Default.Star)
)

// ==========================================
// MAIN APP COMPOSABLE
// ==========================================

@Composable
fun MainAppScreen(viewModel: AppViewModel) {
    var activeTab by remember { mutableStateOf(0) }
    var isSOSOpen by remember { mutableStateOf(false) }
    var selectedSOSSituation by remember { mutableStateOf<Situation?>(null) }
    
    // Auto updating live timer state
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(key1 = true) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .shadow(12.dp),
                containerColor = CardWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("হোম", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("home_tab_button")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.FlashOn, contentDescription = "SOS", tint = AlertRed) },
                    label = { Text("🚨 SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AlertRed) },
                    modifier = Modifier.testTag("sos_tab_button")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Favorite, contentDescription = "Health") },
                    label = { Text("স্বাস্থ্য", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("health_tab_button")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.Lightbulb, contentDescription = "Tips") },
                    label = { Text("টিপস", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tips_tab_button")
                )
                NavigationBarItem(
                    selected = activeTab == 4,
                    onClick = { activeTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("সেটিংস", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("settings_tab_button")
                )
            }
        }
    ) { innerPadding ->
        // Contain matching within max width for premium layout centered
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LightBg)
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 480.dp)
                    .align(Alignment.TopCenter)
            ) {
                when (activeTab) {
                    0 -> HomeTabScreen(viewModel, currentTimeMillis)
                    1 -> SOSTabScreen(
                        viewModel = viewModel,
                        onTriggerSOS = { isSOSOpen = true }
                    )
                    2 -> HealthTabScreen(viewModel, currentTimeMillis)
                    3 -> TipsTabScreen()
                    4 -> SettingsTabScreen(viewModel)
                }

                // ==========================================
                // EMERGENCY HEALTH GREEN-TEAL SOS OVERLAY
                // ==========================================
                AnimatedVisibility(
                    visible = isSOSOpen,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    SOSOverlayScreen(
                        viewModel = viewModel,
                        selectedSituation = selectedSOSSituation,
                        onSelectSituation = { selectedSOSSituation = it },
                        onBack = {
                            if (selectedSOSSituation != null) {
                                selectedSOSSituation = null
                            } else {
                                isSOSOpen = false
                            }
                        },
                        onClose = {
                            selectedSOSSituation = null
                            isSOSOpen = false
                        }
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 1: HOME COMPOSABLE
// ==========================================

@Composable
fun HomeTabScreen(viewModel: AppViewModel, currentTimeMillis: Long) {
    val context = LocalContext.current
    val quitTimestamp = viewModel.quitTimestamp

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // App Identity Hero Top Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = "Eco Logo",
                            tint = AmberAccent,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ধূমপানমুক্ত জীবন",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "একটি সুখী, স্বাস্থ্যোজ্জ্বল ও বিষমুক্ত নতুন দিন",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    // Motivational Islamic Islamic / Spiritual Context Card
                    Text(
                        text = "“এবং নিজের জীবনকে ধ্বংসের মুখে ঠেলে দিও না।”",
                        color = SoftAmber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "[আল-কুরআন, সূরা আল-বাকারা: ১৯৫]",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Live Counter State Logic
        if (quitTimestamp == 0L) {
            // First Launch / Quit Session Installer Setup
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(6.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(SoftAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Select Time",
                                tint = TealPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "আপনার সুস্থ জীবন শপথ শুরু করুন",
                            color = DarkText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ধূমপানমুক্ত দীর্ঘস্থায়ী জীবনের সংকল্প নিয়ে আপনার শেষ ধূমপানের সময়কালটি নির্ধারণ করুন।",
                            color = MutedText,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                val datePickerDialog = DatePickerDialog(
                                    context,
                                    { _, year, month, day ->
                                        val timePickerDialog = TimePickerDialog(
                                            context,
                                            { _, hour, minute ->
                                                val resCal = Calendar.getInstance().apply {
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, day)
                                                    set(Calendar.HOUR_OF_DAY, hour)
                                                    set(Calendar.MINUTE, minute)
                                                    set(Calendar.SECOND, 0)
                                                    set(Calendar.MILLISECOND, 0)
                                                }
                                                viewModel.setQuitTime(resCal.timeInMillis)
                                            },
                                            calendar.get(Calendar.HOUR_OF_DAY),
                                            calendar.get(Calendar.MINUTE),
                                            false
                                        )
                                        timePickerDialog.show()
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                datePickerDialog.show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("start_quit_button")
                        ) {
                            Text("আমি শেষ সিগারেট খেয়েছি ⏱️", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                    }
                }
            }
        } else {
            // Live Counter Active Dashboard Display
            item {
                val elapsedMillis = maxOf(0L, currentTimeMillis - quitTimestamp)
                val days = elapsedMillis / (24 * 3600 * 1000L)
                val hours = (elapsedMillis % (24 * 3600 * 1000L)) / (3600 * 1000L)
                val minutes = (elapsedMillis % (3600 * 1000L)) / (60 * 1000L)
                val seconds = (elapsedMillis % (60 * 1000L)) / 1000L

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "ধূমপানমুক্ত থাকার লাইভ সময়",
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TimeTickerBlock(label = "দিন", value = days.toString())
                            TimeTickerBlock(label = "ঘণ্টা", value = hours.toString())
                            TimeTickerBlock(label = "মিনিট", value = minutes.toString())
                            TimeTickerBlock(label = "সেকেন্ড", value = seconds.toString())
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = BorderGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        val sdf = SimpleDateFormat("dd MMMM, yyyy - hh:mm a", Locale("bn", "BD"))
                        val dateString = sdf.format(Date(quitTimestamp))
                        Text(
                            text = "সংকল্প শুরুর সময়: ${toBengaliNum(dateString)}",
                            fontSize = 12.sp,
                            color = MutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Milestone Target & Motivational Progress Bar
            item {
                val elapsedMillis = maxOf(0L, currentTimeMillis - quitTimestamp)
                
                // Find currently underway milestone
                var underwayMilestone: BodyMilestone? = null
                var previousTargetMillis = 0L
                for (milestone in HEALTH_MILESTONES) {
                    if (elapsedMillis < milestone.durationMillis) {
                        underwayMilestone = milestone
                        break
                    } else {
                        previousTargetMillis = milestone.durationMillis
                    }
                }

                if (underwayMilestone != null) {
                    val progressFraction = if (underwayMilestone.durationMillis > previousTargetMillis) {
                        ((elapsedMillis - previousTargetMillis).toFloat() / (underwayMilestone.durationMillis - previousTargetMillis)).coerceIn(0f, 1f)
                    } else {
                        1f
                    }
                    val percentage = (progressFraction * 100).toInt()

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "চলমান স্বাস্থ্য লক্ষ্য",
                                    color = DarkText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .background(SoftAmber, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = underwayMilestone.durationText,
                                        color = AmberAccent,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = underwayMilestone.title,
                                color = TealPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = underwayMilestone.description,
                                color = MutedText,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // Visual linear progress indicator
                            LinearProgressIndicator(
                                progress = progressFraction,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(CircleShape),
                                color = TealPrimary,
                                trackColor = BorderGray
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "অগ্রগতি: ${toBengaliNum(percentage.toString())}%",
                                    fontSize = 11.sp,
                                    color = TealPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "মাইলস্টোন সম্পূর্ণ হতে অবশিষ্ট সময় ট্র্যাক করুন।",
                                    fontSize = 10.sp,
                                    color = MutedText
                                )
                            }
                        }
                    }
                } else {
                    // All Milestones Achieved (User has been smoke free for 10+ years!)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = SoftAmber),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.WorkspacePremium,
                                contentDescription = "Champion",
                                tint = AmberAccent,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "অভিনন্দন! আপনি চূড়ান্ত বিজয়ী!",
                                color = DarkText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "আপনি ধূমপানের সকল দীর্ঘমেয়াদী কুফল কাটিয়ে একটি ১০০% নিরোগ জীবন লাভ করেছেন।",
                                color = DarkText,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Quick Access to Achievements title
        item {
            Text(
                text = "স্বাস্থ্য পুনরুদ্ধারের মাইলস্টোন ও অর্জন",
                fontSize = 16.sp,
                color = DarkText,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Render health milestones List
        items(HEALTH_MILESTONES) { milestone ->
            val elapsedMillis = if (quitTimestamp > 0L) maxOf(0L, currentTimeMillis - quitTimestamp) else 0L
            val isAchieved = quitTimestamp > 0L && elapsedMillis >= milestone.durationMillis
            val isUnderway = quitTimestamp > 0L && elapsedMillis < milestone.durationMillis &&
                    HEALTH_MILESTONES.indexOf(milestone) == HEALTH_MILESTONES.indexOfFirst { elapsedMillis < it.durationMillis }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isAchieved) Color(0xFFF0FDF4) else if (isUnderway) Color(0xFFFFFBEB) else CardWhite
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isAchieved) Color(0xFFBBF7D0) else if (isUnderway) Color(0xFFFDE68A) else BorderGray
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = if (isAchieved) Color(0xFFBBF7D0) else if (isUnderway) SoftAmber else LightBg,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAchieved) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = TealDark,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = toBengaliNum(milestone.id.toString()),
                                color = if (isUnderway) AmberAccent else MutedText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = milestone.durationText,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isAchieved) TealDark else if (isUnderway) AmberAccent else DarkText,
                                fontSize = 14.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isAchieved) Color(0xFFDCFCE7) else if (isUnderway) SoftAmber else LightBg
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isAchieved) "অর্জিত" else if (isUnderway) "চলমান" else "তালাবদ্ধ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAchieved) TealDark else if (isUnderway) AmberAccent else MutedText
                                )
                            }
                        }
                        Text(
                            text = milestone.title,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = milestone.description,
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeTickerBlock(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(LightBg, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp)
            .width(62.dp)
    ) {
        Text(
            text = toBengaliNum(value),
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TealPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MutedText,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// TAB 2: EMERGENCY SOS LAUNCHER
// ==========================================

@Composable
fun SOSTabScreen(viewModel: AppViewModel, onTriggerSOS: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val glowSize by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Alert",
            tint = AlertRed,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "হঠাৎ ধূমপানের তীব্র ইচ্ছে হচ্ছে?",
            color = DarkText,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "যখন মনের নিয়ন্ত্রণে শয়তানের প্ররোচনা কিংবা তীব্র নিকোটিনের ক্রেভিং তৈরি হবে, তখনই আল্লাহর প্রশংসা ও নিচে দেওয়া লাল বাটনে আলতো স্পর্শ করুন।",
            color = MutedText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        
        Spacer(modifier = Modifier.height(48.dp))

        // Giant Pulse Glowing Emergency Button
        Box(
            modifier = Modifier
                .size(172.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale)
                .shadow(elevation = glowSize.dp, shape = CircleShape, ambientColor = AlertRed, spotColor = AlertRed)
                .background(AlertRedLight, CircleShape)
                .clickable { onTriggerSOS() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .background(AlertRed, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🚨 SOS",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "জরুরী সাহায্য",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = "১০টি কঠিনতম মানসিক পরিস্থিতিতে আপনাকে সাথে রাখতে ৬০টি চূড়ান্ত বাস্তবসম্মত জীবনরক্ষী অ্যাকশন গাইডলাইন প্রস্তুত!",
            color = TealDark,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==========================================
// GENIUNELY HELPFUL GREEN-TEAL SOS OVERLAY
// ==========================================

@Composable
fun SOSOverlayScreen(
    viewModel: AppViewModel,
    selectedSituation: Situation?,
    onSelectSituation: (Situation) -> Unit,
    onBack: () -> Unit,
    onClose: () -> Unit
) {
    val totalChecked = viewModel.completedActions.size
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F766E)) // Soothing Teal Green to reduce panic and anxiety
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Overlay Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .testTag("back_to_situations_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Text(
                text = if (selectedSituation == null) "পরিস্থিতি ভিত্তিক মোকাবেলা" else selectedSituation.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                textAlign = TextAlign.Center
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .testTag("close_sos_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
        }

        // Context motivation alert banner
        if (selectedSituation == null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = SoftAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সব কাজ একসাথে করার প্রয়োজন নেই, এখান থেকে যেকোনো ১টি সহজ কাজ তাত্ক্ষণিক বেছে নিন।",
                        color = Color.White,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            // Grid list of 10 situations
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(SOS_DATA) { situation ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clickable { onSelectSituation(situation) }
                            .testTag("situation_card_${situation.id}"),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = situation.icon,
                                fontSize = 28.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = situation.title,
                                color = DarkText,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = situation.subtitle,
                                color = MutedText,
                                fontSize = 10.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        } else {
            // Display Action list screen with 6 detailed actions
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF0D9488), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 20.dp, bottom = 40.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SoftAmber, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedSituation.icon,
                                    fontSize = 18.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = selectedSituation.title,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "${selectedSituation.subtitle} - নিচের যেকোনো ১টি কাজ বেছে নিন",
                                    color = SoftAmber,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    items(selectedSituation.actions) { action ->
                        val isChecked = viewModel.completedActions.contains(action.id)
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.toggleActionCompleted(action.id) }
                                .shadow(2.dp, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked) Color(0xFFF0FDF4) else CardWhite
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isChecked) Color(0xFF86EFAC) else Color.Transparent
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Number Badge
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (isChecked) Color(0xFF22C55E) else TealPrimary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = toBengaliNumId(action.id),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = action.text,
                                    color = if (isChecked) Color(0xFF166534) else DarkText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                // Checkbox to mark as done
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { viewModel.toggleActionCompleted(action.id) },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF22C55E)),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .testTag("action_checkbox_${action.id}")
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 3: HEALTH INFORMATION & TIMELINE
// ==========================================

@Composable
fun HealthTabScreen(viewModel: AppViewModel, currentTimeMillis: Long) {
    var selectedSubTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // 3 Sub tabs Selector / Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(BorderGray, RoundedCornerShape(10.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SubTabPillButton(
                text = "ক্ষতিকর প্রভাব",
                isSelected = selectedSubTab == 0,
                onClick = { selectedSubTab = 0 }
            )
            SubTabPillButton(
                text = "ছাড়ার কারণ",
                isSelected = selectedSubTab == 1,
                onClick = { selectedSubTab = 1 }
            )
            SubTabPillButton(
                text = "শরীরের উন্নতি",
                isSelected = selectedSubTab == 2,
                onClick = { selectedSubTab = 2 }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Display contents based on subtab selection
        when (selectedSubTab) {
            0 -> HarmfulEffectsContent()
            1 -> ReasonsToQuitContent()
            2 -> BodyRecoveryTimelineContent(viewModel, currentTimeMillis)
        }
    }
}

@Composable
fun RowScope.SubTabPillButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .weight(1f)
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) TealPrimary else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else DarkText,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun HarmfulEffectsContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = AlertRedLight),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Alert",
                        tint = AlertRed,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "তামাক ও ধোঁয়ায় ত্বরান্বিত ক্ষতিগুলো আমাদের ফুসফুস ও মস্তিস্ককে স্থায়ী বিষাক্ততার দিকে ঠেলে দেয়।",
                        color = Color(0xFF991B1B),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        items(HARMFUL_EFFECTS) { effect ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFEF2F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = effect.icon,
                            contentDescription = effect.title,
                            tint = AlertRed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = effect.title,
                            fontWeight = FontWeight.ExtraBold,
                            color = AlertRed,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = effect.description,
                            color = DarkText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ReasonsToQuitContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SoftAmber),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Faith",
                        tint = AmberAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "একটি সুখী, অপচয়হীন জীবন ও আল্লাহর সন্তুষ্টি আদায় করতে ধূমপান বর্জনের সুবর্ণ সংকল্প রাখুন।",
                        color = Color(0xFF92400E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        items(QUIT_REASONS) { reason ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFEF3C7), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = reason.icon,
                            contentDescription = reason.title,
                            tint = AmberAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = reason.title,
                            fontWeight = FontWeight.ExtraBold,
                            color = TealPrimary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = reason.description,
                            color = DarkText,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BodyRecoveryTimelineContent(viewModel: AppViewModel, currentTimeMillis: Long) {
    val quitTimestamp = viewModel.quitTimestamp
    val elapsedMillis = if (quitTimestamp > 0L) maxOf(0L, currentTimeMillis - quitTimestamp) else 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timeline,
                        contentDescription = "Timeline Icon",
                        tint = TealPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "সময়ের সাথে সাথে আপনার শরীরে হওয়া অলৌকিক নিরাময়গুলির চলমান সচিত্র তালিকা নিচে দেখুন।",
                        color = TealDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        items(HEALTH_MILESTONES) { milestone ->
            val isAchieved = quitTimestamp > 0L && elapsedMillis >= milestone.durationMillis

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Left Vertical Timeline Axis Indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(50.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(
                                color = if (isAchieved) TealPrimary else BorderGray,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isAchieved) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Done",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                text = "⏳",
                                fontSize = 11.sp
                            )
                        }
                    }
                    // Visual axis line connection
                    if (HEALTH_MILESTONES.indexOf(milestone) != HEALTH_MILESTONES.size - 1) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(56.dp)
                                .background(if (isAchieved) TealPrimary else BorderGray)
                        )
                    }
                }
                
                // Right Content details
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isAchieved) Color(0xFFF0FDF4) else CardWhite
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isAchieved) Color(0xFFBBF7D0) else BorderGray
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = milestone.durationText,
                                fontWeight = FontWeight.Bold,
                                color = if (isAchieved) TealPrimary else DarkText,
                                fontSize = 13.sp
                            )
                            if (isAchieved) {
                                Text(
                                    text = "অর্জিত",
                                    color = TealDark,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = milestone.title,
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            fontSize = 12.sp
                        )
                        Text(
                            text = milestone.description,
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// TAB 4: TIPS & EXPERT ACTIVITIES
// ==========================================

@Composable
fun TipsTabScreen() {
    var filterCategory by remember { mutableStateOf("all") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "দৈনিক সুস্থতা ও শান্তিময় অভ্যাস সমূহ",
            color = DarkText,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "ধূমপানের তীব্র ইচ্ছেকে পরাস্ত করতে ইতিবাচক মানসিকতা গঠন করুন।",
            color = MutedText,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Dynamic Filtering Pills row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterTabPill(text = "সব", isSelected = filterCategory == "all", onClick = { filterCategory = "all" })
            FilterTabPill(text = "শারীরিক", isSelected = filterCategory == "physical", onClick = { filterCategory = "physical" })
            FilterTabPill(text = "মানসিক", isSelected = filterCategory == "mental", onClick = { filterCategory = "mental" })
            FilterTabPill(text = "আধ্যাত্মিক", isSelected = filterCategory == "spiritual", onClick = { filterCategory = "spiritual" })
            FilterTabPill(text = "সামাজিক", isSelected = filterCategory == "social", onClick = { filterCategory = "social" })
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter and display list
        val displayedTips = if (filterCategory == "all") {
            TIPS_DATA
        } else {
            TIPS_DATA.filter { it.category == filterCategory }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(displayedTips) { tip ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGray)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = when (tip.category) {
                                        "physical" -> Color(0xFFE0F2FE)
                                        "mental" -> Color(0xFFF3E8FF)
                                        "spiritual" -> Color(0xFFECFDF5)
                                        else -> Color(0xFFFFF7ED)
                                    },
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = tip.icon,
                                contentDescription = tip.title,
                                tint = when (tip.category) {
                                    "physical" -> Color(0xFF0284C7)
                                    "mental" -> Color(0xFF9333EA)
                                    "spiritual" -> Color(0xFF059669)
                                    else -> Color(0xFFEA580C)
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = tip.title,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkText,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = tip.description,
                                color = MutedText,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterTabPill(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (isSelected) TealPrimary else BorderGray,
                shape = RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else DarkText,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )
    }
}

// ==========================================
// TAB 5: PREFERENCES AND SETTINGS
// ==========================================

@Composable
fun SettingsTabScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "অ্যাপ সেটিংস ও নিয়ন্ত্রণ প্যানেল",
                color = DarkText,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "আপনার ধূমপান বর্জনের সংকল্প সময় পুনর্বিন্যাস করুন।",
                color = MutedText,
                fontSize = 12.sp
            )
        }

        // Section 1: Session Controller Configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "যাত্রা পুনর্বিন্যাস",
                        fontWeight = FontWeight.Bold,
                        color = TealPrimary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Set/Reset date time picker
                    Button(
                        onClick = {
                            val calendar = Calendar.getInstance()
                            val datePickerDialog = DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    val timePickerDialog = TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            val resCal = Calendar.getInstance().apply {
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, day)
                                                set(Calendar.HOUR_OF_DAY, hour)
                                                set(Calendar.MINUTE, minute)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            viewModel.setQuitTime(resCal.timeInMillis)
                                        },
                                        calendar.get(Calendar.HOUR_OF_DAY),
                                        calendar.get(Calendar.MINUTE),
                                        false
                                    )
                                    timePickerDialog.show()
                                },
                                calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH),
                                calendar.get(Calendar.DAY_OF_MONTH)
                            )
                            datePickerDialog.show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightBg),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.EditCalendar, contentDescription = "Date picker", tint = TealPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.quitTimestamp == 0L) "ছেড়ে দেওয়ার সময় সেট করুন" else "ছেড়ে দেওয়ার সময় পরিবর্তন করুন",
                            color = TealPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = BorderGray)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Daily cigarette count Configuration Slider
                    Text(
                        text = "ধূমপান ত্যাগের পূর্বের দৈনিক সেবন (পরিচিতি):",
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        fontSize = 12.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "দৈনিক ${toBengaliNum(viewModel.dailyCigaretteCount.toString())} টি শলাকা",
                            fontWeight = FontWeight.ExtraBold,
                            color = AmberAccent,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = viewModel.dailyCigaretteCount.toFloat(),
                            onValueChange = { viewModel.setCigaretteCount(it.toInt()) },
                            valueRange = 1f..50f,
                            modifier = Modifier.width(180.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = AmberAccent,
                                activeTrackColor = AmberAccent,
                                inactiveTrackColor = BorderGray
                            )
                        )
                    }
                }
            }
        }

        // Section 2: UI Switch Configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "দৈনিক অনুপ্রেরণামূলক বিজ্ঞপ্তি",
                            fontWeight = FontWeight.Bold,
                            color = DarkText,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "প্রতিদিন সকালে নিয়মত তওবা ও সুস্থতার টিপস নোটিফিকেশন এ পান।",
                            color = MutedText,
                            fontSize = 10.sp,
                            lineHeight = 14.sp
                        )
                    }
                    Switch(
                        checked = viewModel.notificationEnabled,
                        onCheckedChange = { viewModel.setNotifications(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TealPrimary,
                            checkedTrackColor = TealPrimary.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }

        // Section 3: App info reference
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(1.dp, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGray)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "সহায়তা ও তথ্য",
                        fontWeight = FontWeight.Bold,
                        color = DarkText,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "তামাকজনিত চরম ফুসফুসীয় ব্যাধি অথবা তীব্র প্রত্যাহার অনিদ্রা দেখা দিলে অনুগ্রহ করে জাতীয় বক্ষব্যাধি ইনস্টিটিউট ও হাসপাতালের সাহায্য নিন।",
                        color = MutedText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "অনলাইন সাপোর্ট: ১৬২৬৩ (স্বাস্থ্য বাতায়ন)",
                        fontWeight = FontWeight.Bold,
                        color = TealDark,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Section 4: Hard Reset Trigger Dialog
        item {
            Button(
                onClick = { showResetDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = AlertRedLight),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("reset_all_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteForever,
                    contentDescription = "Reset Icon",
                    tint = AlertRed
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "সব তথ্য রিসেট করুন",
                    color = AlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "তথ্য রিসেট নিশ্চিতকরণ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিতভাবে সবকিছু রিসেট করতে চান? আপনার অর্জিত সফল দিন ও অগ্রগতি মুছে যাবে এবং অ্যাপ প্রথম লঞ্চ স্ট্যাটে ফিরে যাবে।",
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetApp()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AlertRed)
                ) {
                    Text("হ্যাঁ, রিসেট করুন", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("বাতিল করুন", color = DarkText)
                }
            }
        )
    }
}
