package com.eina.app.domain

/**
 * Superset: due o piu' esercizi eseguiti a giro, senza recupero in mezzo. Il legame e' un numero
 * di gruppo salvato su ogni esercizio (`supersetGroup`), uguale per i membri dello stesso giro:
 * cosi' un allenamento puo' contenere piu' superset diversi e un esercizio ne cambia con una
 * scrittura sola.
 *
 * Il numero non si mostra mai: a schermo il gruppo si legge come lettera (A, B, C…) nell'ordine
 * in cui compare nella lista, che e' quello che l'utente vede.
 */
object Superset {

    /** Numero di gruppo libero: il piu' alto in uso piu' uno, cosi' non si riusa una lettera viva. */
    fun nextGroup(existing: List<Int?>): Int = (existing.filterNotNull().maxOrNull() ?: 0) + 1

    /** Lettera per ogni gruppo, assegnata nell'ordine in cui i gruppi compaiono nella lista. */
    fun letters(groupsInOrder: List<Int?>): Map<Int, String> {
        val distinct = groupsInOrder.filterNotNull().distinct()
        return distinct.mapIndexed { index, group ->
            group to ('A' + (index % 26)).toString()
        }.toMap()
    }

    /**
     * Sposta di una posizione l'esercizio [movedId], portandosi dietro i compagni di giro.
     *
     * Un superset si sposta tutto insieme: muovere un solo membro lo staccherebbe dal blocco e
     * il giro non si leggerebbe piu' come una sequenza. Fuori da un superset e' il solito
     * scambio fra vicini.
     */
    fun moveBlock(members: List<Member>, movedId: Long, delta: Int): List<Member> {
        if (delta == 0) return members
        val blocks = blocksOf(members)
        val index = blocks.indexOfFirst { block -> block.any { it.id == movedId } }
        val target = index + delta
        if (index < 0 || target !in blocks.indices) return members
        return blocks.toMutableList()
            .apply { add(target, removeAt(index)) }
            .flatten()
    }

    /** La lista spezzata in blocchi: un superset e' un blocco solo, gli altri esercizi uno a testa. */
    private fun blocksOf(members: List<Member>): List<List<Member>> {
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
     * Scioglie i giri rimasti con un solo esercizio. Serve dopo un'eliminazione: tolto il
     * compagno, chi resta non sta facendo un superset con nessuno.
     */
    fun dissolveOrphans(members: List<Member>): List<Member> {
        val counts = members.mapNotNull { it.group }.groupingBy { it }.eachCount()
        return members.map { member ->
            if (member.group != null && (counts[member.group] ?: 0) < 2) member.copy(group = null) else member
        }
    }

    /** Un esercizio della lista, ridotto a quel che serve per comporre i giri. */
    data class Member(val id: Long, val group: Int?)

    /**
     * Lista rimessa in ordine dopo che [movedId] e' entrato nel gruppo [group] (o ne e' uscito
     * con `null`).
     *
     * Due regole, ed entrambe servono a tenere il superset leggibile:
     * - chi entra in un giro si sposta subito dopo l'ultimo dei suoi compagni, perche' un
     *   superset e' una sequenza e non un insieme sparso per la lista;
     * - un giro rimasto con un solo esercizio si scioglie. Fa eccezione il gruppo appena
     *   scelto: "Nuovo superset" nasce per forza con un membro solo, e va lasciato crescere.
     *
     * L'ordine di ritorno e' quello nuovo: la posizione nella lista *e'* il campo `order`.
     */
    fun regroup(members: List<Member>, movedId: Long, group: Int?): List<Member> {
        val movedIndex = members.indexOfFirst { it.id == movedId }
        if (movedIndex < 0) return members

        val assigned = members.map { if (it.id == movedId) it.copy(group = group) else it }
        val reordered = if (group == null) {
            // Uscendo dal giro l'esercizio resta dov'era: spostarlo sarebbe un movimento
            // inspiegabile per chi guarda.
            assigned
        } else {
            // I membri del giro si compattano in blocco dove comincia il giro, con l'ultimo
            // arrivato in coda: cosi' non restano estranei incastrati fra un compagno e l'altro.
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
