package com.lifeos.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifeos.admin.domain.entity.AdminAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminAuditLogMapper extends BaseMapper<AdminAuditLog> {
}
