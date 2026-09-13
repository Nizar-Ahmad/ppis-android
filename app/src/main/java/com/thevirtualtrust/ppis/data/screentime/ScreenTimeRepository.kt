package com.thevirtualtrust.ppis.data.screentime

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.screentime.remote.ScreenTimeApi
import com.thevirtualtrust.ppis.data.screentime.remote.dto.ScreenTimeCreateRequestDto
import com.thevirtualtrust.ppis.data.screentime.remote.dto.ScreenTimeResponseDto
import com.thevirtualtrust.ppis.data.screentime.remote.dto.ScreenTimeUpdateRequestDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenTimeRepository @Inject constructor(
    private val screenTimeApi:
        ScreenTimeApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getAll():
        ApiResult<List<ScreenTimeEntry>> =
        apiCallExecutor.execute {

            screenTimeApi
                .getScreenTimeList()
                .map {
                    it.toDomain()
                }
        }

    suspend fun getByDate(
        entryDate: LocalDate
    ): ApiResult<ScreenTimeEntry> =
        apiCallExecutor.execute {

            screenTimeApi
                .getScreenTime(
                    entryDate.toString()
                )
                .toDomain()
        }

    suspend fun create(
        entryDate: LocalDate,
        values: ScreenTimeValues
    ): ApiResult<ScreenTimeEntry> =
        apiCallExecutor.execute {

            screenTimeApi
                .createScreenTime(
                    ScreenTimeCreateRequestDto(
                        entryDate =
                            entryDate.toString(),
                        totalMinutes =
                            values.totalMinutes,
                        nightMinutes =
                            values.nightMinutes
                    )
                )
                .toDomain()
        }

    suspend fun update(
        entryDate: LocalDate,
        values: ScreenTimeValues
    ): ApiResult<ScreenTimeEntry> =
        apiCallExecutor.execute {

            screenTimeApi
                .updateScreenTime(
                    entryDate =
                        entryDate.toString(),
                    request =
                        ScreenTimeUpdateRequestDto(
                            totalMinutes =
                                values.totalMinutes,
                            nightMinutes =
                                values.nightMinutes
                        )
                )
                .toDomain()
        }

    suspend fun delete(
        entryDate: LocalDate
    ): ApiResult<Unit> =
        apiCallExecutor.execute {

            screenTimeApi
                .deleteScreenTime(
                    entryDate.toString()
                )
        }

    private fun ScreenTimeResponseDto.toDomain():
        ScreenTimeEntry =
        ScreenTimeEntry(
            id = id,
            entryDate =
                LocalDate.parse(
                    entryDate
                ),
            totalMinutes =
                totalMinutes,
            nightMinutes =
                nightMinutes,
            createdAt =
                createdAt,
            updatedAt =
                updatedAt
        )
}
