package com.eina.app.ui.library

import com.eina.app.data.db.WeightType

fun WeightType.label(): String = when (this) {
    WeightType.FREE_WEIGHT -> "Peso libero"
    WeightType.BODYWEIGHT -> "Corpo libero"
    WeightType.BODYWEIGHT_PLUS_LOAD -> "Corpo libero + sovraccarico"
    WeightType.ASSISTED -> "Assistito"
    WeightType.MACHINE_STACK -> "Macchina a pacchi"
    WeightType.TIME_BASED -> "A tempo"
}
