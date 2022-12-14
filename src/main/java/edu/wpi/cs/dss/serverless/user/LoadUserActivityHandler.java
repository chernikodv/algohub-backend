package edu.wpi.cs.dss.serverless.user;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import edu.wpi.cs.dss.serverless.generic.GenericResponse;
import edu.wpi.cs.dss.serverless.user.http.LoadUserActivityRequest;
import edu.wpi.cs.dss.serverless.user.http.LoadUserActivityResponse;
import edu.wpi.cs.dss.serverless.user.model.UserActivity;
import edu.wpi.cs.dss.serverless.util.DataSource;
import edu.wpi.cs.dss.serverless.util.ErrorMessage;
import edu.wpi.cs.dss.serverless.util.HttpStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LoadUserActivityHandler implements RequestHandler<LoadUserActivityRequest, GenericResponse> {
    
    private static final String SQL_UNION_STATEMENT = " UNION ";

    private LambdaLogger logger;

    @Override
    public GenericResponse handleRequest(LoadUserActivityRequest request, Context context) {
        logger = context.getLogger();
        logger.log("Received a get user activity request from AWS Lambda:\n" + request);

        // get user activity
        final GenericResponse response = getActivity(request);
        logger.log("Sent a get user activity response to AWS Lambda:\n" + response);

        return response;
    }

    private GenericResponse getActivity(LoadUserActivityRequest request) {
        final String username = request.getUsername();
        final String activityQuery = "SELECT id, name, 'classification' AS type FROM classification WHERE author_id=?" +
                SQL_UNION_STATEMENT +
                "SELECT id, name, 'algorithm' AS type FROM algorithm WHERE author_id=?" +
                SQL_UNION_STATEMENT +
                "SELECT id, name, 'implementation' AS type FROM implementation WHERE author_id=?" +
                SQL_UNION_STATEMENT +
                "SELECT id, problem_type as name, 'problem_instance' AS type FROM problem_instance WHERE author_id=?" +
                SQL_UNION_STATEMENT +
                "SELECT id, CONCAT(cpu_name, '-', execution_time, '-', memory_usage, ' MB') as name, 'benchmark' AS type FROM benchmark WHERE author_id=?";

        try (final Connection connection = DataSource.getConnection(logger);
             final PreparedStatement activityPreparedStatement = connection.prepareStatement(activityQuery))
        {
            logger.log("Successfully connected to db!");

            activityPreparedStatement.setString(1, username);
            activityPreparedStatement.setString(2, username);
            activityPreparedStatement.setString(3, username);
            activityPreparedStatement.setString(4, username);
            activityPreparedStatement.setString(5, username);

            final List<UserActivity> activity = new ArrayList<>();
            try (final ResultSet resultSet = activityPreparedStatement.executeQuery()) {
                while (resultSet.next()) {
                    final String id = resultSet.getString(1);
                    final String name = resultSet.getString(2);
                    final String typeName = resultSet.getString(3);

                    activity.add(new UserActivity(id, name, typeName));
                }
            }

            return LoadUserActivityResponse.builder()
                    .statusCode(HttpStatus.SUCCESS.getValue())
                    .activity(activity)
                    .build();

        } catch (SQLException e) {
            logger.log(ErrorMessage.SQL_EXECUTION_EXCEPTION.getValue());
            return GenericResponse.builder()
                    .statusCode(HttpStatus.BAD_REQUEST.getValue())
                    .error(ErrorMessage.SQL_EXECUTION_EXCEPTION.getValue())
                    .build();
        }
    }
}
