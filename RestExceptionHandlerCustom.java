package com.dxc.sgisanita.sharedlib.exceptions.handler;

import com.dxc.sgisanita.sharedlib.drools.exception.RuleBusinessException;
import com.dxc.sgisanita.sharedlib.exceptions.apierror.ApiError;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
public class RestExceptionHandlerCustom extends RestExceptionHandler {

    /**
     * Handle rule business exception.
     */
    @ExceptionHandler(RuleBusinessException.class)
    protected ResponseEntity<Object> handleAccessDeniedException(RuleBusinessException ex) {

        ApiError apiError = new ApiError(ex.getStatus(), ex);

        if (!CollectionUtils.isEmpty(ex.getSubMessage())) {

            Set<FieldError> fieldErrorList = new HashSet<>();

            ex.getSubMessage().forEach(errore -> {
                if (StringUtils.isNotBlank(errore.getCode()) &&
                        StringUtils.isNotBlank(errore.getMessage()) &&
                        StringUtils.isNotBlank(errore.getField())) {
                    fieldErrorList.add(new FieldError(errore.getCode(), errore.getField(), errore.getMessage()));
                }
            });
            apiError.addValidationErrors(new ArrayList<>(fieldErrorList));
        }

        apiError.setDebugMessage(ex.getRuleMessage());
        apiError.setMessage(ex.getRuleMessage());

        return buildResponseEntity(apiError, ex);
    }


    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<Object> handleRuntimeException(RuntimeException ex) {
        ApiError apiError = new ApiError(HttpStatus.INTERNAL_SERVER_ERROR);
        apiError.setMessage("Errore non previsto. Contattare il supporto tecnico fornendo il traceId.");
        return buildResponseEntity(apiError, ex);
    }

}
