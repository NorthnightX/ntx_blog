package com.ntx.blog.utils;

import cn.hutool.core.bean.BeanUtil;
import com.ntx.blog.domain.TBlog;
import com.ntx.blog.dto.BlogDTO;
import com.ntx.blog.service.TLikeBlogService;
import com.ntx.common.client.BlogTypeClient;
import com.ntx.common.client.UserClient;
import com.ntx.common.domain.TBlogType;
import com.ntx.common.domain.TUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
@Service
public class PopulatingBlogDTO {
    @Autowired
    private BlogTypeClient blogTypeClient;
    @Autowired
    private UserClient userClient;
    public List<BlogDTO> PopulatingBlogDTOData(List<TBlog> list){
        List<Integer> userIdList = list.stream().map(TBlog::getBlogger).collect(Collectors.toList());
        List<Integer> typeIdList = list.stream().map(TBlog::getTypeId).collect(Collectors.toList());
        Map<Integer, TUser> userMap =
                userClient.getByIds(userIdList).stream().collect(Collectors.toMap(TUser::getId, tUser -> tUser));
        Map<Integer, TBlogType> blogTypeMap = blogTypeClient.
                getByTypeIds(typeIdList).stream().collect(Collectors.toMap(TBlogType::getId, blogType -> blogType));
        return list.stream().map(blog -> {
            BlogDTO blogDTO = new BlogDTO();
            BeanUtil.copyProperties(blog, blogDTO);
            TUser user = userMap.get(blog.getBlogger());
            TBlogType tBlogType = blogTypeMap.get(blog.getTypeId());
            blogDTO.setBloggerId(user.getId());
            blogDTO.setBloggerName(user.getNickName());
            blogDTO.setBloggerImage(user.getImage());
            blogDTO.setTypeName(tBlogType.getName());
            return blogDTO;
        }).collect(Collectors.toList());
    }
}
