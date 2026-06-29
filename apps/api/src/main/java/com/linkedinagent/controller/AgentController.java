package com.linkedinagent.controller;

import com.linkedinagent.dto.response.AgentLogResponse;
import com.linkedinagent.entity.enums.AgentStatus;
import com.linkedinagent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @GetMapping("/logs")
    public ResponseEntity<Page<AgentLogResponse>> getAgentLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(agentService.getAgentLogs(page, size));
    }

    @GetMapping("/logs/agent/{agentName}")
    public ResponseEntity<Page<AgentLogResponse>> getAgentLogsByName(
            @PathVariable String agentName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(agentService.getAgentLogsByName(agentName, page, size));
    }

    @GetMapping("/logs/status/{status}")
    public ResponseEntity<Page<AgentLogResponse>> getAgentLogsByStatus(
            @PathVariable AgentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(agentService.getAgentLogsByStatus(status, page, size));
    }
}
