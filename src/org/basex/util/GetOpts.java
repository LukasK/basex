package org.basex.util;

/**
 * This class splits the input String into its arguments and checks if
 * there is a path expression.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-08, ISC License
 * @author Hannes Schwarz - Hannes.Schwarz@gmail.com
 * @version 0.1
 */
public final class GetOpts {
  /** Argument of an option is stored here. */
  private String optarg;
  /** The path expression is stored here. */
  private String path;
  /** Index of option to be checked. */
  private int optindex;
  /** Index of option to be checked. */
  private int multipleOptIndex;
  /** The valid short options. */
  private String optString;
  /** Arguments found. */
  private StringList foundArgs;
  /** Arguments passed to the program. */
  private String[] args;
  /** The variable optopt saves the last known option
   * character returned by getopt(). */
  private int optopt = 0;
  /** optreset must be set to 1 before the second and each
   * additional set of calls to getopt(), and the variable
   * optindex must be reinitialized. */
  private final int optReset = 1;

  /**
   * Construct a basic Getopt instance with the given input data.
   *
   * @param arguments The String passed from the command line
   *
   * @param options A String containing a description of the
   *                  valid options
   */
  public GetOpts(final String arguments, final String options) {
    args = arguments.split(" ");
    optString = options;
    optindex = 0;
    optarg = null;
    path = null;
    multipleOptIndex = 1;
    foundArgs = new StringList();
  }
  /**
   * Construct a basic Getopt instance with the given input data.
   *
   * @param arguments The String passed from the command line
   * @param options A String containing a description of the
   *                  valid options
   * @param firstOptionIndex start scanning at this index
   */
  public GetOpts(final String arguments, final String options,
      final int firstOptionIndex) {
    args = arguments.split(" ");
    optString = options;
    optindex = firstOptionIndex;
    optarg = null;
    path = null;
    multipleOptIndex = 1;
    foundArgs = new StringList();
  }

  /**
   * Getter of the index.
   *
   * @return optindex - Index of the next option to be checked. Returns
   *                    -1 if it is at the end of the optString.
   */
  public int getOptind() {
    return optindex;
  }

  /**
   * For communication to the caller. No set method
   * is provided because setting this variable has no effect.
   *
   * @return When an option is found it is stored in optarg
   * and returned here.
   */
  public String getOptarg() {
    return optarg;
  }

  /**
   * getFoundArgs is used to return all parsed
   * arguments like source_file target_file.
   *
   * @return all parsed arguments
   */
  public StringList getFoundArgs() {
    return foundArgs;
  }

  /**
   * getPath is used to store a path expression.
   *
   * @return path to go
   */
  public String getPath() {
    return path;
  }

  /**
   * Checks if more options can be returned.
   * @return result of check
   */
  public boolean more() {
    return getopt() != -1;
  }
  
  /**
   * Returns the next option.
   * @return next option
   */
  public int next() {
    return optopt;
  }

  /**
   * This method checks the string passed from the command line.
   *
   * If an option is found it returns it and store possible arguments
   * in optarg. If an invalid option is found, 0 is returned and an
   * error thrown. If there is no more to be checked -1 will be returned.
   *
   * @return see above
   */
  public int getopt() {
    optarg = null;
    // parsed all input
    if(optindex >= args.length) {
      optopt = -1;
      return optopt;
    }

    String arg = args[optindex];
    int argLength = arg.length();
    // option found
    if(arg.startsWith("-") && argLength > 1) {
      //get argument
      optopt = args[optindex].charAt(multipleOptIndex);
      int optPos = optString.indexOf(optopt);
      // valid option ?
      if(optPos > -1) {
        if(optString.length() - 1  > optPos &&
            optString.charAt(optPos + 1) == ':') {

          if(argLength > multipleOptIndex + 1) {
            // e.g. -oArgument
            optindex++;
            optarg = arg.substring(multipleOptIndex + 1, argLength);
          } else if(args.length > optindex + 1) {
            optindex += 2;
            optarg = args[optindex - 1];
          } else {
            optindex += 2;
            optarg = ":";
          }
          // if(optString.charAt(optPos + 2) == ':')
          // not yet implemented - optional argument
          
          /* no argument allowed if argLength is bigger than
           * multipleOptIndex + 1 (=2) there must be another
           * option -> just return this option / increment multipleOptIndex */
        } else if(argLength == multipleOptIndex + 1) {
          optindex++;
          multipleOptIndex = 1;
        } else {
          multipleOptIndex++;
        }
        return optopt;
      }
      
      // Unknown option -> any options left ? set pointer
      if(argLength == multipleOptIndex + 1) { // e.g. -X (X = unknown)
        optindex++;
        multipleOptIndex = 1;
      } else {  // e.g. -yXa (y,a = known / X = unknown)
        multipleOptIndex++;
      }
      optopt = 0;
      return optopt;
    }

    if(args.length > 0 && args[optindex].length() > 0) {
      // path or nonvalid option
      path = args[optindex];
      foundArgs.add(args[optindex]);
    }
    
    // all options parsed
    if(optindex + 1 == args.length) {
      optopt = -1;
      return optopt;
    }

    // return next option
    ++optindex;
    return getopt();
  }

  /**
   * In order to use getopt() to evaluate multiple sets of arguments, or to
   * evaluate a single set of arguments multiple times, the variable optreset
   * must be set to 1 before the second and each additional set of calls to
   * getopt(), and the variable optindex must be reinitialized.
   */
  public void reset() {
    optindex = optReset;
  }
}
