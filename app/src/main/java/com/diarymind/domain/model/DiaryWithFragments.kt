package com.diarymind.domain.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class DiaryWithFragments(
    @Embedded val diary: DiaryEntry,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            FragmentDiaryCrossRef::class,
            parentColumn = "diaryId",
            entityColumn = "fragmentId"
        )
    )
    val fragments: List<Fragment>
)
