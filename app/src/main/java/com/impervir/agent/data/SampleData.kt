package com.impervir.agent.data

/**
 * The shared sample world (persona: Maya Chen, sells Notion templates).
 * Every screen reads these exact numbers so they agree with each other and with the iOS design.
 */
object SampleData {
    const val userName = "Maya Chen"
    const val userInitials = "MC"
    const val today = "Monday, Oct 20"
    const val thisWeek = 43
    const val lastWeek = 42
    const val lastWeekRange = "Oct 13 to 19"

    val products = listOf("Focus Planner" to 29, "Weekly Review Kit" to 19)

    val week42Posts = listOf(
        Post("p42-1", Platform.LinkedIn, "Customer story: \"finally stopped missing deadlines\"", "She told me she finally stopped missing client deadlines. Eleven days with the planner.", "Tue", "9:10 AM", PostStatus.Posted, VoiceSource("Customer story", "0:06"), stats = PostStats(8420, 312, 47, 212, 12, 340)),
        Post("p42-2", Platform.Threads, "Behind the scenes: the page I almost cut", "Her favourite part was the page I almost cut.", "Wed", "12:30 PM", PostStatus.Posted, VoiceSource("The page I almost cut", "0:48"), stats = PostStats(3150, 188, 22, 96, 4, 120)),
        Post("p42-3", Platform.X, "Quick tips: 3 ways to plan a week", "3 ways to plan a week without burning out.", "Thu", "8:45 AM", PostStatus.Posted, stats = PostStats(5980, 141, 9, 40, 0, 0)),
        Post("p42-4", Platform.Telegram, "Focus Planner restock", "Focus Planner is back.", "Mon", "6:00 PM", PostStatus.Posted, stats = PostStats(1180, null, null, 31, 2, 58)),
        Post("p42-5", Platform.Telegram, "Weekly Review Kit how-to", "How I run my Sunday review in 20 minutes.", "Wed", "6:00 PM", PostStatus.Posted, stats = PostStats(1140, null, null, 27, 1, 19)),
        Post("p42-6", Platform.Telegram, "Customer screenshot", "A customer's week, planned.", "Fri", "6:00 PM", PostStatus.Posted, stats = PostStats(1090, null, null, 22, 1, 29)),
        Post("p42-7", Platform.Telegram, "Sunday reset tip", "One question to end the week.", "Sun", "6:00 PM", PostStatus.Posted, stats = PostStats(1020, null, null, 18, 0, 0)),
    )

    val week42 = WeekTotals(
        week = 42, range = lastWeekRange, views = 21_980, clicks = 446, orders = 20, revenue = 566, clickToOrder = 4.5,
        revenueByPlatform = linkedMapOf(Platform.LinkedIn to 340, Platform.Threads to 120, Platform.Telegram to 106, Platform.X to 0),
    )
    const val customerStoriesShare = 60 // % of revenue

    val revenueTrend = listOf(35 to 120, 36 to 180, 37 to 95, 38 to 240, 39 to 310, 40 to 280, 41 to 415, 42 to 566)
    val viewsTrend = listOf(6_200, 7_900, 7_100, 10_400, 12_800, 13_500, 17_200, 21_980)
    val editRateTrend = listOf(38 to 34, 39 to 27, 40 to 21, 41 to 16, 42 to 11)

    val followers = listOf(
        Follower(Platform.LinkedIn, 4_812, 126),
        Follower(Platform.X, 2_340, 41),
        Follower(Platform.Threads, 1_905, 63),
        Follower(Platform.Telegram, 1_240, 38),
    )

    val bestTimes = mapOf(
        Platform.LinkedIn to "Tue/Wed 9 AM",
        Platform.Threads to "12 to 1 PM",
        Platform.X to "8 AM",
        Platform.Telegram to "6 PM",
    )

    val stories = listOf(
        Story("s1", "A customer finally stopped missing client deadlines", "0:06"),
        Story("s2", "Eleven days with the Notion planner", "0:21"),
        Story("s3", "Her favourite part was the one I almost cut", "0:48"),
        Story("s4", "Launch email had a broken link; the sorry note got more replies than the launch", "1:12"),
        Story("s5", "Building a Weekly Review template next", "1:40"),
    )

    val week43Drafts = listOf(
        Post("p43-1", Platform.LinkedIn, "Customer story: missed deadlines",
            "A customer told me she used to miss one client deadline a week.\n\nEleven days after switching to the Focus Planner, she hasn't missed one.\n\nShe didn't get more disciplined. She just stopped keeping deadlines in her head.\n\nWhat do you keep in your head that belongs on paper?",
            "Tue", "9:10 AM", PostStatus.NeedsApproval, VoiceSource("from your voice note", "0:06"), checksPassed = 4),
        Post("p43-2", Platform.Threads, "The page I almost cut",
            "I almost cut one page from the Focus Planner.\n\nIt's the one customers mention most.",
            "Wed", "12:30 PM", PostStatus.NeedsApproval, VoiceSource("from your voice note", "0:48")),
        Post("p43-3", Platform.X, "11 days",
            "11 days. That's how long it took a customer to stop missing deadlines.\n\nNo new habits. One page a morning.",
            "Thu", "8:45 AM", PostStatus.NeedsApproval, VoiceSource("from your voice note", "0:21")),
        Post("p43-4", Platform.Telegram, "Weekly Review Kit is coming", "Building a Weekly Review template next. Want early access?", "Mon", "6:00 PM", PostStatus.AutoPost, VoiceSource("from your voice note", "1:40")),
        Post("p43-5", Platform.Telegram, "The sorry note", "Our launch email had a broken link. The sorry note got more replies than the launch.", "Wed", "6:00 PM", PostStatus.AutoPost, VoiceSource("from your voice note", "1:12")),
        Post("p43-6", Platform.Telegram, "Customer win", "Eleven days, zero missed deadlines.", "Fri", "6:00 PM", PostStatus.AutoPost),
        Post("p43-7", Platform.Telegram, "Sunday reset", "One question to end your week: what would you redo?", "Sun", "6:00 PM", PostStatus.AutoPost),
    )

    /** Open times the user can fill (AM slots included). */
    val openSlots = listOf(Slot("Mon", "8:30 AM", true), Slot("Wed", "7:45 AM", true), Slot("Sat", "10:00 AM", true))

    val comments = listOf(
        Comment("c1", "Priya Nair", "PN", Platform.LinkedIn, "Customer story", "This is so relatable. Does the planner work for teams too?", "12m", true,
            listOf("Right now it's built for one person, but a few customers share a page with their team. Want me to send you how they set it up?", "Not yet for teams. What would you want it to do for yours?")),
        Comment("c2", "Tom Becker", "TB", Platform.LinkedIn, "Customer story", "Eleven days is fast. What changed first?", "25m", true,
            listOf("The morning page. She wrote her three deadlines down before opening email.", "Honestly, one page a morning. That's it.")),
        Comment("c3", "Ana Ruiz", "AR", Platform.LinkedIn, "Customer story", "Saving this for Monday.", "41m", true,
            listOf("Thanks Ana. Tell me how your Monday goes.", "Hope it helps. The first page is the one that matters.")),
        Comment("c4", "Sam Okafor", "SO", Platform.LinkedIn, "Customer story", "How is this different from a normal Notion template?", "1h", false,
            listOf("It's built around one daily question instead of a dashboard. Happy to show you.", "Fewer pages, one habit. That's the difference.")),
        Comment("c5", "Lena Fischer", "LF", Platform.LinkedIn, "Customer story", "Love the honesty in this.", "2h", false,
            listOf("Thank you, Lena. It's her story, I just got to tell it.", "Thanks. She was happy to let me share it.")),
        Comment("c6", "Priya Nair", "PN", Platform.Threads, "The page I almost cut", "Which page was it?", "3h", false,
            listOf("The weekly 'what slipped' page. I thought nobody would use it.", "The one where you write what slipped. Funny, right?")),
        Comment("c7", "Tom Becker", "TB", Platform.Threads, "The page I almost cut", "Always the way.", "4h", false,
            listOf("Every time. Now I ask customers before cutting anything.", "Lesson learned.")),
        Comment("c8", "Sam Okafor", "SO", Platform.X, "Quick tips", "Tip 2 is underrated.", "5h", false,
            listOf("Agreed. It's the one I use most.", "Thanks Sam. Which one do you use?")),
    )

    val voiceRules = listOf(
        VoiceRule("r1", "Never says 'game-changer'"),
        VoiceRule("r2", "Short first line, no question hooks"),
        VoiceRule("r3", "Uses numbers instead of adjectives"),
        VoiceRule("r4", "Ends with a real question"),
        VoiceRule("r5", "No emojis on LinkedIn"),
        VoiceRule("r6", "Says 'customers', not 'users'"),
    )
    const val voiceMatch = 89 // %

    val checkInQuestions = listOf(
        "What happened with customers this week?",
        "Anything you almost cut, changed or got wrong?",
        "What are you building next?",
    )
}
