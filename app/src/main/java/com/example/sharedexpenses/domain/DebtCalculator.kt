package com.example.sharedexpenses.domain

/** One expense as the calculator sees it: who paid, how much, and each person's share (all in øre). */
data class ExpenseSplit(
    val payerId: String,
    val amountOre: Long,
    val shares: Map<String, Long>,
)

/** "from" owes "to" this amount. */
data class Settlement(
    val fromId: String,
    val toId: String,
    val amountOre: Long,
)

/**
 * F10: algorithm for what each member owes or is owed in a group.
 * Pure Kotlin (no Android) so it is easy to unit test.
 */
object DebtCalculator {

    /**
     * Splits [amountOre] evenly between [userIds]. Leftover øre (when it does not divide evenly)
     * go one each to the first members, so the shares always add up to the full amount.
     */
    fun splitEvenly(amountOre: Long, userIds: List<String>): Map<String, Long> {
        require(userIds.isNotEmpty()) { "Need at least one participant" }
        val base = amountOre / userIds.size
        val remainder = amountOre % userIds.size
        return userIds.mapIndexed { index, id ->
            id to base + if (index < remainder) 1 else 0
        }.toMap()
    }

    /**
     * Net balance per member: what they paid minus their shares.
     * Positive = the group owes them. Negative = they owe the group. All balances sum to 0.
     */
    fun netBalances(expenses: List<ExpenseSplit>, memberIds: Collection<String> = emptyList()): Map<String, Long> {
        val net = memberIds.associateWith { 0L }.toMutableMap()
        for (expense in expenses) {
            net[expense.payerId] = (net[expense.payerId] ?: 0L) + expense.amountOre
            for ((userId, share) in expense.shares) {
                net[userId] = (net[userId] ?: 0L) - share
            }
        }
        return net
    }

    /**
     * Turns net balances into a short list of payments (greedy):
     * the person who owes the most pays the person who is owed the most, repeat until everyone is at 0.
     * Gives at most (members - 1) payments.
     */
    fun settle(net: Map<String, Long>): List<Settlement> {
        val debtors = net.filterValues { it < 0 }.mapValues { -it.value }.toMutableMap()
        val creditors = net.filterValues { it > 0 }.toMutableMap()
        val result = mutableListOf<Settlement>()
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            val (debtor, owes) = debtors.maxBy { it.value }
            val (creditor, owed) = creditors.maxBy { it.value }
            val amount = minOf(owes, owed)
            result += Settlement(fromId = debtor, toId = creditor, amountOre = amount)
            if (owes == amount) debtors.remove(debtor) else debtors[debtor] = owes - amount
            if (owed == amount) creditors.remove(creditor) else creditors[creditor] = owed - amount
        }
        return result
    }
}
