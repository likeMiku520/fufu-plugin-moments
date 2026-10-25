package run.halo.moments;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Request body of updating the upvote count of a moment.
 *
 * @author fufu
 * @since 1.0.1
 */
@Data
public class MomentUpvoteRequest {

    @Schema(requiredMode = REQUIRED, description = "Upvote count to set for the moment")
    private Integer upvote;
}