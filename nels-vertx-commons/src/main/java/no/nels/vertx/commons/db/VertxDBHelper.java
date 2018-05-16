package no.nels.vertx.commons.db;


import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;


/**
 * Created by weizhang on 3/21/16.
 */
public class VertxDBHelper {

    public JDBCClient getJdbcClient() {
        return jdbcClient;
    }

    private JDBCClient jdbcClient;
    private static Logger logger = LogManager.getLogger(VertxDBHelper.class);

    private Vertx vertx;

    public VertxDBHelper(Vertx vertx, String url, String user, String driver_class, String password, int max_pool_size) throws IOException {
        this.vertx = vertx;

        jdbcClient = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", url)
                .put("driver_class", driver_class)
                .put("user", user)
                .put("password", password)
                .put("max_pool_size", max_pool_size));

    }

    public void select(String sql, JsonArray params, Handler<AsyncResult<String>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();

                connection.queryWithParams(sql, params, select -> {
                    if (select.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(select.cause()));
                    } else {
                        JsonArray result = new JsonArray(select.result().getRows());
                        asyncResultHandler.handle(Future.succeededFuture(result.encodePrettily()));
                    }
                    connection.close();
                });


            }
        });
    }

    public void select(String sql, Handler<AsyncResult<String>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.query(sql, select -> {
                    if (select.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(select.cause()));
                    } else {
                        JsonArray result = new JsonArray(select.result().getRows());
                        asyncResultHandler.handle(Future.succeededFuture(result.encodePrettily()));
                    }
                    connection.close();
                });


            }
        });
    }

    public void count(String sql, JsonArray params, Handler<AsyncResult<Integer>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.queryWithParams(sql, params, rs -> {
                    if (rs.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(rs.cause()));
                    } else {
                        asyncResultHandler.handle(Future.succeededFuture(rs.result().getRows().get(0).getInteger("count")));

                    }
                    connection.close();
                });

            }
        });
    }

    public void count(String sql, Handler<AsyncResult<Integer>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.query(sql,rs -> {
                    if (rs.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(rs.cause()));
                    } else {
                        asyncResultHandler.handle(Future.succeededFuture(rs.result().getRows().get(0).getInteger("count")));

                    }
                    connection.close();
                });

            }
        });
    }


    public void insert(String sql, JsonArray params, Handler<AsyncResult<Long>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();

                connection.updateWithParams(sql, params, res -> {
                    if (res.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(res.cause()));
                    } else {
                        logger.debug("Insert return result: " + res.result().toJson().encode());
                        Long id = res.result().getKeys().getLong(0);
                        asyncResultHandler.handle(Future.succeededFuture(id));
                    }
                    connection.close();
                });

            }
        });
    }

    public void insert(String sql, JsonArray params, Handler<AsyncResult<Void>> asyncResultHandler, Supplier<Boolean> supplier) {
        updateWithExtraFunction(sql, params, asyncResultHandler, supplier);
    }

    public void updateMultipleSqlInTransaction(Handler<AsyncResult<Void>> asyncResultHandler, BiConsumer<SQLConnection, Handler<AsyncResult<Void>>> consumer) {
        jdbcClient.getConnection(connection -> {
            if (connection.succeeded()) {
                SQLConnection sqlConnection = connection.result();
                sqlConnection.setAutoCommit(false, autoCommitResult -> {
                    if (autoCommitResult.succeeded()) {
                        consumer.accept(sqlConnection, finalResult -> {
                            if (finalResult.succeeded()) {
                                sqlConnection.commit(commitResult -> {
                                    sqlConnection.close();
                                    if (commitResult.succeeded()) {
                                        asyncResultHandler.handle(Future.succeededFuture());
                                    } else {
                                        asyncResultHandler.handle(Future.failedFuture(commitResult.cause()));
                                    }

                                });
                            } else {
                                sqlConnection.rollback(rollbackResult -> {
                                    sqlConnection.close();
                                    asyncResultHandler.handle(Future.failedFuture(finalResult.cause()));
                                });
                            }
                        });
                    } else {
                        sqlConnection.close();
                        asyncResultHandler.handle(Future.failedFuture(autoCommitResult.cause()));
                    }
                });
            } else {
                asyncResultHandler.handle(Future.failedFuture(connection.cause()));
            }
        });
    }

    private void updateWithExtraFunction(String sql, JsonArray params, Handler<AsyncResult<Void>> asyncResultHandler, Supplier<Boolean> supplier) {
        logger.debug("Receive update request with extra function");
        logger.debug(sql);
        logger.debug(params.encode());
        jdbcClient.getConnection(result -> {
            if (result.succeeded()) {
                SQLConnection connection = result.result();
                connection.setAutoCommit(false, result1 -> {
                    if (result1.succeeded()) {
                        connection.updateWithParams(sql, params, result2 -> {
                            if (result2.succeeded()) {
                                vertx.executeBlocking(future -> {
                                    try {
                                        if (supplier.get()) {
                                            future.complete(true);
                                        } else {
                                            future.fail("Function fails");
                                        }
                                    } catch (Exception e) {
                                        logger.error(e);
                                        future.fail(e.getCause());
                                    }
                                }, false, asyncResult -> {
                                    if (asyncResult.succeeded()) {
                                        connection.commit(result3 -> {
                                            if (result3.succeeded()) {
                                                asyncResultHandler.handle(Future.succeededFuture());
                                            } else {
                                                //TODO the function has been done in storage system, but committing db operation failed
                                                asyncResultHandler.handle(Future.failedFuture(result3.cause()));
                                            }
                                            connection.close();
                                        });
                                    } else {
                                        logger.error(asyncResult.cause());
                                        connection.rollback(result3 -> {
                                            if (result3.succeeded()) {
                                                asyncResultHandler.handle(Future.failedFuture("Function can't be done"));
                                            } else {
                                                //TODO the db could not do rollback
                                                asyncResultHandler.handle(Future.failedFuture(result3.cause()));
                                            }
                                            connection.close();
                                        });
                                    }
                                });
                            } else {
                                connection.close();
                                asyncResultHandler.handle(Future.failedFuture(result2.cause()));
                            }
                        });
                    } else {
                        connection.close();
                        asyncResultHandler.handle(Future.failedFuture(result1.cause()));
                    }
                });
            } else {
                asyncResultHandler.handle(Future.failedFuture(result.cause()));
            }
        });
    }

    public void existOne(String sql, JsonArray params, Handler<AsyncResult<Boolean>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();

                connection.queryWithParams(sql, params, res -> {
                    if (res.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(res.cause()));
                    } else {
                        asyncResultHandler.handle(Future.succeededFuture(res.result().getNumRows() == 1 ? true : false));
                    }
                    connection.close();
                });

            }
        });
    }

    public void getOne(String sql, JsonArray params, Handler<AsyncResult<String>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.queryWithParams(sql, params, res -> {
                    if (res.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(res.cause()));
                    } else {
                        if(res.result().getNumRows() >= 1) {
                            asyncResultHandler.handle(Future.succeededFuture(res.result().getRows().get(0).encodePrettily()));
                        }else {
                            asyncResultHandler.handle(Future.succeededFuture(""));
                        }
                    }
                    connection.close();
                });


            }
        });
    }

    public void update(String sql, JsonArray params, Handler<AsyncResult<Void>> asyncResultHandler, Supplier<Boolean> supplier) {
        updateWithExtraFunction(sql, params, asyncResultHandler, supplier);
    }

    public void updateOne(String sql, JsonArray params, Handler<AsyncResult<String>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.updateWithParams(sql, params, res -> {
                    if (res.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(res.cause()));
                    } else {
                        asyncResultHandler.handle(Future.succeededFuture(res.result().toJson().encodePrettily()));
                    }
                    connection.close();
                });

            }
        });
    }

    public void update(String sql, Handler<AsyncResult<String>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();
                connection.update(sql, res -> {
                    if (res.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(res.cause()));
                    } else {
                        asyncResultHandler.handle(Future.succeededFuture(res.result().toJson().encodePrettily()));
                    }
                    connection.close();
                });

            }
        });
    }

    public void deleteOne(String sql, Handler<AsyncResult<String>> asyncResultHandler) {
        jdbcClient.getConnection(ar -> {
            if (ar.failed()) {
                asyncResultHandler.handle(Future.failedFuture(ar.cause()));
            } else {
                SQLConnection connection = ar.result();

                connection.execute(sql, res -> {
                    if (res.failed()) {
                        asyncResultHandler.handle(Future.failedFuture(res.cause()));
                    } else {
                        asyncResultHandler.handle(Future.succeededFuture(String.valueOf(res.succeeded())));
                    }
                    connection.close();
                });

            }
        });
    }

    public void delete(String sql, JsonArray params, Handler<AsyncResult<Void>> asyncResultHandler) {
        jdbcClient.getConnection(result -> {
            if (result.succeeded()) {
                SQLConnection connection = result.result();
                connection.updateWithParams(sql, params, sqlResult -> {
                    if (sqlResult.succeeded()) {
                        asyncResultHandler.handle(Future.succeededFuture());
                    } else {
                        asyncResultHandler.handle(Future.failedFuture(sqlResult.cause()));
                    }
                });
            } else {
                asyncResultHandler.handle(Future.failedFuture(result.cause()));
            }
        });
    }

    public void delete(String sql, Handler<AsyncResult<Void>> asyncResultHandler) {
        jdbcClient.getConnection(result -> {
            if (result.succeeded()) {
                SQLConnection connection = result.result();
                connection.update(sql, sqlResult -> {
                    if (sqlResult.succeeded()) {
                        asyncResultHandler.handle(Future.succeededFuture());
                    } else {
                        asyncResultHandler.handle(Future.failedFuture(sqlResult.cause()));
                    }
                });
            } else {
                asyncResultHandler.handle(Future.failedFuture(result.cause()));
            }
        });
    }

    public void delete(String sql, JsonArray params, Handler<AsyncResult<Void>> asyncResultHandler, Supplier<Boolean> supplier) {
        updateWithExtraFunction(sql, params, asyncResultHandler, supplier);
    }


}
