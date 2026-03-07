package com.chat.message.repository

import com.chat.message.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository // <--- ESTA ANOTACIÓN ES CLAVE
interface UserRepository : JpaRepository<User, String>