package uk.gov.hmcts.reform.cwrdapi.controllers.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@NoArgsConstructor
public class UserProfileCreationResponse {

    private String idamId;
    private Integer idamRegistrationResponse;

    public void setIdamRegistrationResponse(Integer idamRegistrationResponse) {
        this.idamRegistrationResponse = idamRegistrationResponse;
    }

    public boolean isUserCreated() {
        return getIdamRegistrationResponse() == HttpStatus.CREATED.value();
    }
}
