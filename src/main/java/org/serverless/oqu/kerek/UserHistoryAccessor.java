package org.serverless.oqu.kerek;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import org.serverless.oqu.kerek.model.BookInfo;
import org.serverless.template.ApiGatewayEventHandler;

import java.util.List;

import static java.util.Collections.emptyList;

public class UserHistoryAccessor extends ApiGatewayEventHandler<String, List<BookInfo>> {

    static {
        initBookRepository();
    }

    public UserHistoryAccessor() {
        super(String.class);
    }

    @Override
    protected String getRequestData(APIGatewayProxyRequestEvent input) {
        return null;
    }

    @Override
    protected List<BookInfo> doHandleRequest(String input, Context context) {
        try {
            log(context, "Starting fetch books history for user (email = %s)", email());

            final var bookIds = bookRepository.findBookIdsByUserEmail(email());
            return bookRepository.findByBookIds(bookIds);

        } catch (Exception e) {
            e.printStackTrace();
            log(context, "Error occurred while books history for the user with email %s: %s", email(), e.getMessage());
        }
        return emptyList();
    }
}
