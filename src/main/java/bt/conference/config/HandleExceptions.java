package bt.conference.config;

import com.fierhub.model.BaseResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class HandleExceptions  {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse> handleGlobalException(Exception ex) {
        return new ResponseEntity<>(BaseResponse.RaiseError("Error", ex), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<BaseResponse> handleApplicationException(ApplicationException ex) {
        return new ResponseEntity<>(BaseResponse.RaiseError("Error", ex), HttpStatus.BAD_REQUEST);
    }
}
