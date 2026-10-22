package com.example.data.model

import java.util.UUID

data class ProposalAction(
    val taskId: Long,
    val taskTitle: String,
    val actionType: String, // "RESCHEDULE", "SPLIT_MICROSTEPS", "POSTPONE_TO_WEEKEND"
    val description: String,
    val newDueDate: String? = null,
    val newDueTime: String? = null,
    val microStepsToAdd: List<String> = emptyList()
)

data class AiPlanProposal(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val rationale: String,
    val proposedActions: List<ProposalAction>
)
