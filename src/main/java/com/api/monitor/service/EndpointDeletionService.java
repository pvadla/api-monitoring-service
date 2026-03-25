package com.api.monitor.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.api.monitor.entity.Endpoint;
import com.api.monitor.entity.User;
import com.api.monitor.repository.EndpointCheckRepository;
import com.api.monitor.repository.EndpointRepository;
import com.api.monitor.repository.IncidentRepository;

import lombok.RequiredArgsConstructor;

/**
 * Deletes HTTP endpoints and dependent rows in a single transaction.
 * Uses bulk JPQL deletes so foreign keys are cleared before removing the endpoint row.
 */
@Service
@RequiredArgsConstructor
public class EndpointDeletionService {

    private final EndpointRepository endpointRepository;
    private final EndpointCheckRepository endpointCheckRepository;
    private final IncidentRepository incidentRepository;

    @Transactional
    public void deleteOwnedEndpoint(Long endpointId, User user) {
        Endpoint endpoint = endpointRepository.findById(endpointId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not found"));
        if (!endpoint.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Endpoint not found");
        }
        Long id = endpoint.getId();
        endpointCheckRepository.deleteAllByEndpointId(id);
        incidentRepository.deleteAllByEndpointId(id);
        endpointRepository.deleteById(id);
    }
}
