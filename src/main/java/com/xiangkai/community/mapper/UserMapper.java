package com.xiangkai.community.mapper;

import com.xiangkai.community.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    User selectById(Integer id);

    User selectByUserName(String username);

    User selectByEmail(String email);

    Integer insertUser(User user);

    Integer updateStatus(Integer id, Integer status);

    Integer updateHeaderUrl(Integer id, String headerUrl);

    Integer updatePassword(Integer id, String password);

}