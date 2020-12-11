package uk.gov.hmcts.reform.cwrdapi.client.response;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RoleDeletionResponse {
    private String roleName;
    private String idamStatusCode;
    private String idamMessage;
}
