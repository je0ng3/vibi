package com.vibi.shared.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.vibi.shared.data.local.db.entity.EditProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EditProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: EditProjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(projects: List<EditProjectEntity>)

    @Update
    suspend fun update(project: EditProjectEntity)

    @Query("SELECT * FROM edit_projects WHERE projectId = :projectId")
    suspend fun getById(projectId: String): EditProjectEntity?

    @Query("SELECT * FROM edit_projects WHERE projectId = :projectId")
    fun observeById(projectId: String): Flow<EditProjectEntity?>

    /** 메인 화면 "이어서 작업" 카드용 — 현재 로그인 계정의 프로젝트만, 최근 편집 순. */
    @Query("SELECT * FROM edit_projects WHERE userId = :userId ORDER BY updatedAt DESC")
    fun observeAllForUser(userId: String): Flow<List<EditProjectEntity>>

    /** 7일 만료 cleanup 용 — 현재 계정 한정, threshold 미만 updatedAt 의 projectId. */
    @Query("SELECT projectId FROM edit_projects WHERE userId = :userId AND updatedAt < :threshold")
    suspend fun getExpiredIdsForUser(userId: String, threshold: Long): List<String>

    @Query("DELETE FROM edit_projects WHERE projectId = :projectId")
    suspend fun deleteById(projectId: String)
}
