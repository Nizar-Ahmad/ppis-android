package com.thevirtualtrust.ppis.data.activity

import com.thevirtualtrust.ppis.core.network.ApiCallExecutor
import com.thevirtualtrust.ppis.core.network.ApiResult
import com.thevirtualtrust.ppis.data.activity.remote.ActivityApi
import com.thevirtualtrust.ppis.data.activity.remote.dto.ActivityCreateRequestDto
import com.thevirtualtrust.ppis.data.activity.remote.dto.ActivityResponseDto
import com.thevirtualtrust.ppis.data.activity.remote.dto.ActivityUpdateRequestDto
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityApi:
        ActivityApi,
    private val apiCallExecutor:
        ApiCallExecutor
) {

    suspend fun getAll():
        ApiResult<List<ActivityEntry>> =
        apiCallExecutor.execute {

            activityApi
                .getActivities()
                .map {
                    it.toDomain()
                }
        }

    suspend fun getByDate(
        entryDate: LocalDate
    ): ApiResult<ActivityEntry> =
        apiCallExecutor.execute {

            activityApi
                .getActivity(
                    entryDate.toString()
                )
                .toDomain()
        }

    suspend fun create(
        entryDate: LocalDate,
        values: ActivityValues
    ): ApiResult<ActivityEntry> =
        apiCallExecutor.execute {

            activityApi
                .createActivity(
                    ActivityCreateRequestDto(
                        entryDate =
                            entryDate.toString(),
                        steps =
                            values.steps,
                        activityMinutes =
                            values.activityMinutes,
                        source =
                            values.source.apiValue
                    )
                )
                .toDomain()
        }

    suspend fun update(
        entryDate: LocalDate,
        values: ActivityValues
    ): ApiResult<ActivityEntry> =
        apiCallExecutor.execute {

            activityApi
                .updateActivity(
                    entryDate =
                        entryDate.toString(),
                    request =
                        ActivityUpdateRequestDto(
                            steps =
                                values.steps,
                            activityMinutes =
                                values.activityMinutes,
                            source =
                                values.source.apiValue
                        )
                )
                .toDomain()
        }

    private fun ActivityResponseDto.toDomain():
        ActivityEntry =
        ActivityEntry(
            id = id,
            entryDate =
                LocalDate.parse(
                    entryDate
                ),
            steps = steps,
            activityMinutes =
                activityMinutes,
            source =
                ActivitySource
                    .fromApiValue(
                        source
                    ),
            createdAt =
                createdAt,
            updatedAt =
                updatedAt
        )
}
