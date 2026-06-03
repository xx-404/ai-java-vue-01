package com.mars.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mars.common.result.PageResult;
import com.mars.common.result.Result;
import com.mars.system.entity.SysDept;
import com.mars.system.entity.SysPost;
import com.mars.system.entity.SysUser;
import com.mars.system.service.SysDeptService;
import com.mars.system.service.SysPostService;
import com.mars.system.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys/org")
@RequiredArgsConstructor
public class OrgStructureController {

    private final SysDeptService deptService;
    private final SysPostService postService;
    private final SysUserService userService;

    @GetMapping("/tree")
    @SaCheckPermission("sys:dept:list")
    public Result<List<OrgTreeNode>> tree() {
        List<OrgTreeNode> result = new ArrayList<>();

        OrgTreeNode deptRoot = new OrgTreeNode();
        deptRoot.setKey("dept-root");
        deptRoot.setLabel("部门");
        deptRoot.setNodeType("virtual");
        deptRoot.setIsLeaf(false);
        List<SysDept> deptTree = deptService.tree(null, null);
        deptRoot.setChildren(convertDeptNodes(deptTree));
        result.add(deptRoot);

        OrgTreeNode postRoot = new OrgTreeNode();
        postRoot.setKey("post-root");
        postRoot.setLabel("岗位");
        postRoot.setNodeType("virtual");
        postRoot.setIsLeaf(false);
        List<SysPost> postTree = postService.tree();
        postRoot.setChildren(convertPostNodes(postTree));
        result.add(postRoot);

        return Result.ok(result);
    }

    @GetMapping("/users")
    @SaCheckPermission("sys:user:list")
    public Result<List<OrgTreeNode>> users(
            @RequestParam String nodeType,
            @RequestParam Long nodeId) {
        List<OrgTreeNode> userNodes = new ArrayList<>();

        if ("dept".equals(nodeType)) {
            PageResult<SysUser> page = userService.page(1, 200, null, null, null, nodeId, null);
            for (SysUser user : page.getList()) {
                userNodes.add(convertUserNode(user));
            }
        } else if ("post".equals(nodeType)) {
            PageResult<SysUser> page = userService.page(1, 200, null, null, null, null, nodeId);
            for (SysUser user : page.getList()) {
                userNodes.add(convertUserNode(user));
            }
        }

        return Result.ok(userNodes);
    }

    private List<OrgTreeNode> convertDeptNodes(List<SysDept> depts) {
        List<OrgTreeNode> nodes = new ArrayList<>();
        if (depts == null) return nodes;
        for (SysDept dept : depts) {
            OrgTreeNode node = new OrgTreeNode();
            node.setKey("dept-" + dept.getId());
            node.setLabel(dept.getDeptName());
            node.setNodeType("dept");
            node.setNodeId(dept.getId());
            node.setIsLeaf(false);
            node.setExtra(buildDeptExtra(dept));
            if (dept.getChildren() != null && !dept.getChildren().isEmpty()) {
                node.setChildren(convertDeptNodes(dept.getChildren()));
            }
            nodes.add(node);
        }
        return nodes;
    }

    private List<OrgTreeNode> convertPostNodes(List<SysPost> posts) {
        List<OrgTreeNode> nodes = new ArrayList<>();
        if (posts == null) return nodes;
        for (SysPost post : posts) {
            OrgTreeNode node = new OrgTreeNode();
            node.setKey("post-" + post.getId());
            node.setLabel(post.getPostName());
            node.setNodeType("post");
            node.setNodeId(post.getId());
            node.setIsLeaf(false);
            node.setExtra(buildPostExtra(post));
            if (post.getChildren() != null && !post.getChildren().isEmpty()) {
                node.setChildren(convertPostNodes(post.getChildren()));
            }
            nodes.add(node);
        }
        return nodes;
    }

    private OrgTreeNode convertUserNode(SysUser user) {
        OrgTreeNode node = new OrgTreeNode();
        node.setKey("user-" + user.getId());
        node.setLabel(user.getNickname() != null && !user.getNickname().isEmpty() ? user.getNickname() : user.getUsername());
        node.setNodeType("user");
        node.setNodeId(user.getId());
        node.setIsLeaf(true);
        node.setExtra(buildUserExtra(user));
        return node;
    }

    private Map<String, Object> buildDeptExtra(SysDept dept) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("deptName", dept.getDeptName());
        extra.put("parentId", dept.getParentId());
        extra.put("ancestors", dept.getAncestors());
        extra.put("sort", dept.getSort());
        extra.put("leader", dept.getLeader());
        extra.put("phone", dept.getPhone());
        extra.put("email", dept.getEmail());
        extra.put("status", dept.getStatus());
        extra.put("createTime", dept.getCreateTime());
        return extra;
    }

    private Map<String, Object> buildPostExtra(SysPost post) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("postName", post.getPostName());
        extra.put("parentId", post.getParentId());
        extra.put("postCode", post.getPostCode());
        extra.put("sort", post.getSort());
        extra.put("status", post.getStatus());
        extra.put("remark", post.getRemark());
        extra.put("createTime", post.getCreateTime());
        return extra;
    }

    private Map<String, Object> buildUserExtra(SysUser user) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("username", user.getUsername());
        extra.put("nickname", user.getNickname());
        extra.put("avatar", user.getAvatar());
        extra.put("email", user.getEmail());
        extra.put("phone", user.getPhone());
        extra.put("gender", user.getGender());
        extra.put("status", user.getStatus());
        extra.put("userType", user.getUserType());
        extra.put("deptId", user.getDeptId());
        extra.put("deptName", user.getDeptName());
        extra.put("postNames", user.getPostNames());
        extra.put("isQuit", user.getIsQuit());
        extra.put("createTime", user.getCreateTime());
        return extra;
    }

    @Data
    public static class OrgTreeNode {
        private String key;
        private String label;
        private String nodeType;
        private Long nodeId;
        private Boolean isLeaf;
        private Map<String, Object> extra;
        private List<OrgTreeNode> children;
    }
}
