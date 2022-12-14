package edu.wpi.cs.dss.serverless.algorithm;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.wpi.cs.dss.serverless.algorithm.http.LoadAlgorithmResponse;
import edu.wpi.cs.dss.serverless.algorithm.http.ReclassifyAlgorithmRequest;
import edu.wpi.cs.dss.serverless.generic.GenericResponse;
import edu.wpi.cs.dss.serverless.util.DataSource;
import edu.wpi.cs.dss.serverless.util.ErrorMessage;
import edu.wpi.cs.dss.serverless.util.HttpStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ReclassifyAlgorithmHandler implements RequestHandler<ReclassifyAlgorithmRequest, GenericResponse> {

    private LambdaLogger logger;

    @Override
    public GenericResponse handleRequest(ReclassifyAlgorithmRequest request, Context context) {
        logger = context.getLogger();
        logger.log("Received a reclassify algorithm request from AWS Lambda:\n" + request);

        final GenericResponse response = reclassify(request);
        logger.log("Sent a reclassify algorithm response to AWS Lambda:\n" + response);

        return response;
    }

    private GenericResponse reclassify(ReclassifyAlgorithmRequest request) {
        // extracting new and old classification id from reclassify algorithm request
        final String newClassificationId = request.getNewClassificationId();
        final String algorithmId = request.getAlgorithmId();

        // create sql query
        final String query = "UPDATE algorithm SET classification_id = ? WHERE id = ?";

        // execute query
        try (final Connection connection = DataSource.getConnection(logger);
             final PreparedStatement preparedStatement = connection.prepareStatement(query)
        ) {
            logger.log("Successfully connected to db!");

            preparedStatement.setString(1, newClassificationId);
            preparedStatement.setString(2, algorithmId);

            int rowsAffected = preparedStatement.executeUpdate();
            logger.log("Reclassify statement has affected " + rowsAffected + " rows!");

            return GenericResponse.builder()
                    .error("")
                    .statusCode(HttpStatus.SUCCESS.getValue())
                    .build();

        } catch (SQLException e) {
            logger.log(ErrorMessage.SQL_EXECUTION_EXCEPTION.getValue());
            return LoadAlgorithmResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.getValue())
                    .error(ErrorMessage.SQL_EXECUTION_EXCEPTION.getValue())
                    .build();
        }
    }
}
