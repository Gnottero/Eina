package com.eina.app.domain

/**
 * Superset: two or more exercises performed as a round, with no rest in between. The link is a
 * group number stored on each exercise (`supersetGroup`), equal for the members of a round, so a
 * workout can hold several supersets and moving an exercise between them is a single write.
 *
 * The number is never shown: on screen a group reads as a letter (A, B, C…) assigned in the order
 * it appears in the list.
 */
object Superset {

    /** Free group number: the highest in use plus one, so a live letter is never reused. */
    fun nextGroup(existing: List<Int?>): Int = (existing.filterNotNull().maxOrNull() ?: 0) + 1

    /** Letter per group, assigned in the order the groups appear in the list. */
    fun letters(groupsInOrder: List<Int?>): Map<Int, String> {
        val distinct = groupsInOrder.filterNotNull().distinct()
        return distinct.mapIndexed { index, group ->
            group to ('A' + (index % 26)).toString()
        }.toMap()
    }

    /**
     * The list split into blocks: a superset is one block, every other exercise its own. This is
     * the unit of reordering — moving a single member would detach it from its round.
     */
    fun blocksOf(members: List<Member>): List<List<Member>> {
        val blocks = mutableListOf<MutableList<Member>>()
        members.forEach { member ->
            val previous = blocks.lastOrNull()
            if (member.group != null && previous?.firstOrNull()?.group == member.group) {
                previous.add(member)
            } else {
                blocks.add(mutableListOf(member))
            }
        }
        return blocks
    }

    /**
     * Dissolves rounds left with a single exercise, as happens after a deletion: with no companion
     * left, the survivor is not supersetting with anyone.
     */
    fun dissolveOrphans(members: List<Member>): List<Member> {
        val counts = members.mapNotNull { it.group }.groupingBy { it }.eachCount()
        return members.map { member ->
            if (member.group != null && (counts[member.group] ?: 0) < 2) member.copy(group = null) else member
        }
    }

    /** An exercise of the list, reduced to what composing the rounds needs. */
    data class Member(val id: Long, val group: Int?)

    /**
     * The list reordered after [movedId] joined [group], or left it with a `null` group.
     *
     * Two rules, both there to keep a superset readable:
     * - a joining exercise moves right after the last of its companions, since a superset is a
     *   sequence and not a set scattered through the list;
     * - a round left with a single exercise dissolves, except for the group just chosen: a new
     *   superset necessarily starts with one member and must be allowed to grow.
     *
     * The returned order is the new one: the position in the list is the `order` field.
     */
    fun regroup(members: List<Member>, movedId: Long, group: Int?): List<Member> {
        val movedIndex = members.indexOfFirst { it.id == movedId }
        if (movedIndex < 0) return members

        val assigned = members.map { if (it.id == movedId) it.copy(group = group) else it }
        val reordered = if (group == null) {
            // Leaving a round, the exercise stays where it is: moving it would look arbitrary.
            assigned
        } else {
            // Members are compacted into a block where the round begins, with the newcomer last,
            // so no outsider stays wedged between two companions.
            val block = assigned.filter { it.group == group && it.id != movedId } +
                assigned.first { it.id == movedId }
            val rest = assigned.filter { it.group != group }
            val anchor = assigned.indexOfFirst { it.group == group }
            val insertAt = assigned.take(anchor).count { it.group != group }
            rest.toMutableList().apply { addAll(insertAt, block) }
        }

        val counts = reordered.mapNotNull { it.group }.groupingBy { it }.eachCount()
        return reordered.map { member ->
            val orphan = member.group != null && member.group != group && (counts[member.group] ?: 0) < 2
            if (orphan) member.copy(group = null) else member
        }
    }
}
