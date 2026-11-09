package com.example.sharedexpenses

import com.example.sharedexpenses.domain.DebtCalculator
import com.example.sharedexpenses.domain.ExpenseSplit
import com.example.sharedexpenses.domain.Settlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** F10: the debt calculation algorithm. */
class DebtCalculatorTest {

    @Test
    fun splitEvenly_dividesExactly() {
        val shares = DebtCalculator.splitEvenly(300_00, listOf("a", "b", "c"))
        assertEquals(mapOf("a" to 100_00L, "b" to 100_00L, "c" to 100_00L), shares)
    }

    @Test
    fun splitEvenly_leftoverOreGoesToFirstMembers_andSumIsExact() {
        val shares = DebtCalculator.splitEvenly(100_00, listOf("a", "b", "c"))
        assertEquals(3334L, shares["a"])
        assertEquals(3333L, shares["b"])
        assertEquals(3333L, shares["c"])
        assertEquals(100_00L, shares.values.sum())
    }

    @Test
    fun netBalances_payerIsOwedEveryoneElsesShare() {
        val dinner = ExpenseSplit("alex", 300_00, DebtCalculator.splitEvenly(300_00, listOf("alex", "sam", "mia")))
        val net = DebtCalculator.netBalances(listOf(dinner))
        assertEquals(200_00L, net["alex"])
        assertEquals(-100_00L, net["sam"])
        assertEquals(-100_00L, net["mia"])
        assertEquals(0L, net.values.sum())
    }

    @Test
    fun netBalances_selectedMembersOnly() {
        // Groceries 150 DKK paid by Alex, split only between Alex and Sam (F09 "selected members")
        val groceries = ExpenseSplit("alex", 150_00, DebtCalculator.splitEvenly(150_00, listOf("alex", "sam")))
        val net = DebtCalculator.netBalances(listOf(groceries), memberIds = listOf("alex", "sam", "mia"))
        assertEquals(75_00L, net["alex"])
        assertEquals(-75_00L, net["sam"])
        assertEquals(0L, net["mia"])
    }

    @Test
    fun settle_twoExpenses_givesMinimalPayments() {
        val everyone = listOf("alex", "sam", "mia")
        val dinner = ExpenseSplit("alex", 300_00, DebtCalculator.splitEvenly(300_00, everyone))
        val taxi = ExpenseSplit("sam", 120_00, DebtCalculator.splitEvenly(120_00, everyone))
        // alex +160, sam -20, mia -140
        val settlements = DebtCalculator.settle(DebtCalculator.netBalances(listOf(dinner, taxi)))
        assertEquals(
            listOf(Settlement("mia", "alex", 140_00), Settlement("sam", "alex", 20_00)),
            settlements,
        )
    }

    @Test
    fun settle_everyoneEven_noPayments() {
        assertTrue(DebtCalculator.settle(mapOf("a" to 0L, "b" to 0L)).isEmpty())
    }

    @Test
    fun settle_paymentsCancelAllBalances() {
        val net = mapOf("a" to 500L, "b" to -200L, "c" to -300L, "d" to 250L, "e" to -250L)
        val settlements = DebtCalculator.settle(net)
        val after = net.toMutableMap()
        for (s in settlements) {
            after[s.fromId] = after.getValue(s.fromId) + s.amountOre
            after[s.toId] = after.getValue(s.toId) - s.amountOre
        }
        assertTrue(after.values.all { it == 0L })
        assertTrue(settlements.size <= net.size - 1)
    }
}
