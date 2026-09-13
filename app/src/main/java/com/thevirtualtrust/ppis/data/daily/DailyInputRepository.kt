package com.thevirtualtrust.ppis.data.daily

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.daily.remote.DailyInputApi
import com.thevirtualtrust.ppis.data.daily.remote.dto.DailyInputCreateRequestDto
import com.thevirtualtrust.ppis.data.daily.remote.dto.DailyInputResponseDto
import com.thevirtualtrust.ppis.data.daily.remote.dto.DailyInputUpdateRequestDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyInputRepository @Inject constructor(
    private val dailyInputApi:
        DailyInputApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getAll():
        ApiResult<List<DailyInputEntry>> =
        apiCallExecutor.execute {

            dailyInputApi
                .getDailyInputs()
                .map {
                    it.toDomain()
                }
        }

    suspend fun getByDate(
        entryDate: LocalDate
    ): ApiResult<DailyInputEntry> =
        apiCallExecutor.execute {

            dailyInputApi
                .getDailyInput(
                    entryDate.toString()
                )
                .toDomain()
        }

    suspend fun create(
        entryDate: LocalDate,
        values: DailyInputValues
    ): ApiResult<DailyInputEntry> =
        apiCallExecutor.execute {

            dailyInputApi
                .createDailyInput(
                    DailyInputCreateRequestDto(
                        entryDate =
                            entryDate.toString(),
                        mood =
                            values.mood,
                        sleepHours =
                            values.sleepHours,
                        energyLevel =
                            values.energyLevel,
                        focusedWorkHours =
                            values.focusedWorkHours,
                        notes =
                            normalizeNotes(
                                values.notes
                            )
                    )
                )
                .toDomain()
        }

    suspend fun update(
        entryDate: LocalDate,
        values: DailyInputValues
    ): ApiResult<DailyInputEntry> =
        apiCallExecutor.execute {

            dailyInputApi
                .updateDailyInput(
                    entryDate =
                        entryDate.toString(),
                    request =
                        DailyInputUpdateRequestDto(
                            mood =
                                values.mood,
                            sleepHours =
                                values.sleepHours,
                            energyLevel =
                                values.energyLevel,
                            focusedWorkHours =
                                values.focusedWorkHours,
                            notes =
                                normalizeNotes(
                                    values.notes
                                )
                        )
                )
                .toDomain()
        }

    private fun normalizeNotes(
        value: String?
    ): String? =
        value
            ?.trim()
            ?.takeIf {
                it.isNotBlank()
            }

    private fun DailyInputResponseDto.toDomain():
        DailyInputEntry =
        DailyInputEntry(
            id = id,
            entryDate =
                LocalDate.parse(
                    entryDate
                ),
            mood = mood,
            sleepHours =
                sleepHours,
            energyLevel =
                energyLevel,
            focusedWorkHours =
                focusedWorkHours,
            notes = notes,
            createdAt =
                createdAt,
            updatedAt =
                updatedAt
        )
}
