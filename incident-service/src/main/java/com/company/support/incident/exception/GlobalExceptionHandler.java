package com.company.support.incident.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.List;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        return switch (exception) {
            case NotFoundException e -> buildResponse(
                    Response.Status.NOT_FOUND, "Not Found", e.getMessage());

            case ForbiddenException e -> buildResponse(
                    Response.Status.FORBIDDEN, "Forbidden", "Access denied");

            case NotAuthorizedException e -> buildResponse(
                    Response.Status.UNAUTHORIZED, "Unauthorized", "Authentication required");

            case ConstraintViolationException e -> buildValidationResponse(e);

            case IllegalArgumentException e -> buildResponse(
                    Response.Status.BAD_REQUEST, "Bad Request", e.getMessage());

            default -> {
                LOG.errorf(exception, "Unhandled exception on %s", path());
                yield buildResponse(
                        Response.Status.INTERNAL_SERVER_ERROR,
                        "Internal Server Error",
                        "An unexpected error occurred");
            }
        };
    }

    private Response buildResponse(Response.Status status, String error, String message) {
        return Response.status(status)
                .entity(ErrorResponse.builder()
                        .status(status.getStatusCode())
                        .error(error)
                        .message(message)
                        .path(path())
                        .timestamp(LocalDateTime.now())
                        .build())
                .build();
    }

    private Response buildValidationResponse(ConstraintViolationException e) {
        List<ErrorResponse.FieldError> fields = e.getConstraintViolations().stream()
                .map(cv -> {
                    String field = cv.getPropertyPath().toString();
                    // Тут для красоты убираю префикс: "create.request.name" -> "name"
                    if (field.contains(".")) {
                        field = field.substring(field.lastIndexOf('.') + 1);
                    }
                    return ErrorResponse.FieldError.builder()
                            .field(field)
                            .message(cv.getMessage())
                            .build();
                })
                .toList();

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(ErrorResponse.builder()
                        .status(400)
                        .error("Validation Failed")
                        .message("Request contains invalid fields")
                        .path(path())
                        .timestamp(LocalDateTime.now())
                        .fields(fields)
                        .build())
                .build();
    }

    private String path() {
        try {
            return uriInfo != null ? uriInfo.getPath() : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
