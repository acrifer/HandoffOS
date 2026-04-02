package com.lifeos.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lifeos.admin.domain.entity.AdminUser;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminUserMapper extends BaseMapper<AdminUser> {

    @Select("""
            SELECT r.role_code
            FROM admin_role r
            INNER JOIN admin_user_role ur ON ur.role_id = r.id
            WHERE ur.admin_user_id = #{adminUserId}
            ORDER BY r.role_code
            """)
    List<String> findRoleCodes(Long adminUserId);

    @Insert("""
            INSERT INTO admin_user_role (admin_user_id, role_id)
            VALUES (#{adminUserId}, #{roleId})
            """)
    int bindRole(Long adminUserId, Long roleId);
}
