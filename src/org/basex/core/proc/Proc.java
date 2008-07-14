package org.basex.core.proc;

import static org.basex.Text.*;
import java.io.IOException;
import org.basex.BaseX;
import org.basex.core.AbstractProcess;
import org.basex.core.Command;
import org.basex.core.Commands;
import org.basex.core.Context;
import org.basex.core.Prop;
import org.basex.data.Data;
import org.basex.data.Nodes;
import org.basex.data.Result;
import org.basex.io.CachedOutput;
import org.basex.io.PrintOutput;
import org.basex.query.QueryException;
import org.basex.query.xpath.XPathProcessor;
import org.basex.util.Performance;
import org.basex.util.TokenBuilder;

/**
 * This class provides the architecture for all internal command
 * implementations. It evaluates queries that are sent by the GUI,
 * the client or the standalone version.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Christian Gruen
 */
public abstract class Proc extends AbstractProcess {
  /** Container for query information. */
  private TokenBuilder info = new TokenBuilder();
  /** Performance measurements. */
  protected Performance perf = new Performance();
  /** Current context. */
  protected Context context;
  /** Temporary query result. */
  protected Result result;

  /**
   * Returns a process instance of the specified command.
   * @param context database context
   * @param comm command to be executed
   * @param arg arguments
   * @return process instance
   */
  public static Proc get(final Context context, final Commands comm,
      final String... arg) {
    
    final StringBuilder sb = new StringBuilder();
    for(final String a : arg) sb.append(a + " ");
    return new Command(comm, sb.toString().trim()).proc(context);
  }

  /**
   * Executes the specified command.
   * @param context context
   * @param comm command to be executed
   * @param arg arguments
   * @return success of operation
   */
  public static boolean execute(final Context context, final Commands comm,
      final String... arg) {
    return get(context, comm, arg).execute();
  }

  /**
   * Initializes the process.
   * @param ctx query context
   * @param comm command to be executed
   */
  public final void init(final Context ctx, final Command comm) {
    context = ctx;
    cmd = comm;
  }

  @Override
  public final boolean execute() {
    // database does not exist...
    final Commands comm = cmd.name;
    if(comm.data() && context.data() == null) return error(PROCNODB);
    if(comm.updating() && (Prop.mainmem || Prop.onthefly)) return error(PROCMM);

    try {
      // wrong number of arguments...
      if(comm.args(cmd.nrArgs())) return exec();
      throw new IllegalArgumentException();
    } catch(final IllegalArgumentException ex) {
      BaseX.debug(ex);
      return error(PROCSYNTAX, comm.help(true, true));
    } catch(final Exception ex) {
      // should not happen...
      ex.printStackTrace();
      return error(PROCERR, cmd, ex);
    } catch(final OutOfMemoryError ex) {
      // should not happen...
      ex.printStackTrace();
      Performance.gc(2);
      return error(PROCOUTMEM, cmd, ex);
    } catch(final Error ex) {
      ex.printStackTrace();
      return error(PROCERR, cmd, ex);
    }
  }

  /**
   * Executes a process. Returns an {@link IllegalArgumentException} if
   * the number of arguments is wrong.
   * @return success of operation
   */
  protected abstract boolean exec();

  @Override
  public final void output(final PrintOutput out) throws IOException {
    try {
      if(cmd.name.printing()) out(out);
      else BaseX.debug("No result available for \"%\"", this);
    } catch(final IOException ex) {
      throw ex;
    } catch(final Exception ex) {
      out.print(ex.toString());
      BaseX.debug(ex);
    }
  }

  /**
   * Returns a query result.
   * @param out output stream
   * @throws Exception exception
   */
  @SuppressWarnings("unused")
  protected void out(final PrintOutput out) throws Exception { }

  /**
   * Returns all data generated during the last call of
   * {@link #exec(Commands,String) process} in a single string.
   * Only recommended for short results.
   * @return result string
   */
  public final String output() {
    try {
      final CachedOutput cache = new CachedOutput();
      out(cache);
      return cache.toString();
    } catch(final Exception ex) {
      ex.printStackTrace();
      return null;
    }
  }

  @Override
  public final void info(final PrintOutput out) throws IOException {
    out.print(info.toString());
  }

  /**
   * Returns the query information as a string.
   * @return info string
   */
  public final String info() {
    return info.toString();
  }

  /**
   * Returns the result set, generated by the last query.
   * @return result set
   */
  public final Result result() {
    return result;
  }

  // Common sub methods =======================================================

  /**
   * Executes the specified command, adopting the process results.
   * @param comm command to be executed
   * @param arg arguments
   * @return success of operation
   */
  protected final boolean exec(final Commands comm, final String arg) {
    cmd.arg(arg);
    return exec(comm);
  }

  /**
   * Executes the specified command, adopting the process results.
   * @param comm command to be executed
   * @return success of operation
   */
  protected final boolean exec(final Commands comm) {
    final Proc proc = get(context, comm, cmd.args());
    progress(proc);
    final boolean ok = proc.exec();
    info = proc.info;
    perf = proc.perf;
    result = proc.result;
    return ok;
  }

  /**
   * Adds information on the process execution.
   * @param str information to be added
   * @param ext extended info
   * @return true
   */
  protected final boolean info(final String str, final Object... ext) {
    info.add(str, ext);
    return true;
  }

  /**
   * Adds the error message to the message buffer {@link #info}.
   * @param msg error message
   * @param ext error extension
   * @return false
   */
  protected final boolean error(final String msg, final Object... ext) {
    info.reset();
    info.add(msg == null ? "" : msg, ext);
    return false;
  }

  /**
   * Performs a query for update operations.
   * @param query query to be performed
   * @param err if this string is specified, it is thrown if the results
   * don't yield element nodes
   * @return resulting node set
   */
  protected final Nodes query(final String query, final String err) {
    try {
      final XPathProcessor qu = new XPathProcessor(query);
      progress(qu);
      final Nodes nodes = qu.queryNodes(context.current());
      // check if all result nodes are tags
      if(err != null) {
        final Data data = context.data();
        for(int i = nodes.size - 1; i >= 0; i--) {
          if(data.kind(nodes.pre[i]) != Data.ELEM) {
            error(err);
            return null;
          }
        }
      }
      return nodes;
    } catch(final QueryException ex) {
      BaseX.debug(ex);
      error(ex.getMessage());
      return null;
    }
  }

  @Override
  public final String toString() {
    final StringBuilder sb = new StringBuilder(cmd.name.toString());
    for(int a = 0; a != cmd.nrArgs(); a++) {
      sb.append(" " + cmd.arg(a));
    }
    return sb.toString();
  }
}
