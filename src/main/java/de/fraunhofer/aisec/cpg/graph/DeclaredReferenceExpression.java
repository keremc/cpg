/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *                    $$$$$$\  $$$$$$$\   $$$$$$\
 *                   $$  __$$\ $$  __$$\ $$  __$$\
 *                   $$ /  \__|$$ |  $$ |$$ /  \__|
 *                   $$ |      $$$$$$$  |$$ |$$$$\
 *                   $$ |      $$  ____/ $$ |\_$$ |
 *                   $$ |  $$\ $$ |      $$ |  $$ |
 *                   \$$$$$   |$$ |      \$$$$$   |
 *                    \______/ \__|       \______/
 *
 */

package de.fraunhofer.aisec.cpg.graph;

import de.fraunhofer.aisec.cpg.graph.HasType.TypeListener;
import de.fraunhofer.aisec.cpg.graph.type.Type;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An expression, which refers to something which is declared, e.g. a variable. For example, the
 * expression <code>a = b</code>, which itself is a {@link BinaryOperator}, contains two {@link
 * DeclaredReferenceExpression}s, one for the variable <code>a</code> and one for variable <code>b
 * </code>, which have been previously been declared.
 */
public class DeclaredReferenceExpression extends Expression implements TypeListener {

  /** The {@link ValueDeclaration}s this expression might refer to. */
  private Set<Declaration> refersTo = new HashSet<>();

  /**
   * Is this reference used for writing data instead of just reading it? Determines dataflow
   * direction
   */
  private AccessValues access = AccessValues.READ;

  private boolean staticAccess = false;

  public boolean isStaticAccess() {
    return staticAccess;
  }

  public void setStaticAccess(boolean staticAccess) {
    this.staticAccess = staticAccess;
  }

  public Set<Declaration> getRefersTo() {
    return refersTo;
  }

  public AccessValues getAccess() {
    return access;
  }

  public void setRefersTo(@Nullable Declaration refersTo) {
    if (refersTo == null) {
      return;
    }
    HashSet<Declaration> n = new HashSet<>();
    n.add(refersTo);
    setRefersTo(n);
  }

  public void setRefersTo(@NonNull Set<Declaration> refersTo) {
    this.refersTo.stream()
        .filter(ValueDeclaration.class::isInstance)
        .map(ValueDeclaration.class::cast)
        .forEach(
            r -> {
              if (access == AccessValues.WRITE) {
                this.removeNextDFG(r);
              } else if (access == AccessValues.READ) {
                this.removePrevDFG(r);
              } else {
                this.removeNextDFG(r);
                this.removePrevDFG(r);
              }
              r.unregisterTypeListener(this);
              if (r instanceof TypeListener) {
                this.unregisterTypeListener((TypeListener) r);
              }
            });

    this.refersTo.clear();
    this.refersTo.addAll(refersTo);

    refersTo.stream()
        .filter(ValueDeclaration.class::isInstance)
        .map(ValueDeclaration.class::cast)
        .forEach(
            r -> {
              if (access == AccessValues.WRITE) {
                this.addNextDFG(r);
              } else if (access == AccessValues.READ) {
                this.addPrevDFG(r);
              } else {
                this.addNextDFG(r);
                this.addPrevDFG(r);
              }
              r.registerTypeListener(this);
              if (r instanceof TypeListener) {
                this.registerTypeListener((TypeListener) r);
              }
            });
  }

  @Override
  public void typeChanged(HasType src, HasType root, Type oldType) {
    Type previous = this.type;
    setType(src.getPropagationType(), root);
    if (!previous.equals(this.type)) {
      this.type.setTypeOrigin(Type.Origin.DATAFLOW);
    }
  }

  @Override
  public void possibleSubTypesChanged(HasType src, HasType root, Set<Type> oldSubTypes) {
    Set<Type> subTypes = new HashSet<>(getPossibleSubTypes());
    subTypes.addAll(src.getPossibleSubTypes());
    setPossibleSubTypes(subTypes, root);
  }

  @Override
  public String toString() {
    return new ToStringBuilder(this, Node.TO_STRING_STYLE)
        .append("name", name)
        .append("type", type)
        .append("location", location)
        .append("refersTo", refersTo)
        .toString();
  }

  public void setAccess(AccessValues access) {
    this.refersTo.forEach(
        r -> {
          if (this.access == AccessValues.WRITE) {
            this.removeNextDFG(r);
          } else if (this.access == AccessValues.READ) {
            this.removePrevDFG(r);
          } else {
            this.removeNextDFG(r);
            this.removePrevDFG(r);
          }
        });

    this.access = access;
    refersTo.forEach(
        r -> {
          if (this.access == AccessValues.WRITE) {
            this.addNextDFG(r);
          } else if (this.access == AccessValues.READ) {
            this.addPrevDFG(r);
          } else {
            this.addNextDFG(r);
            this.addPrevDFG(r);
          }
        });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof DeclaredReferenceExpression)) {
      return false;
    }
    DeclaredReferenceExpression that = (DeclaredReferenceExpression) o;
    return super.equals(that) && Objects.equals(refersTo, that.refersTo);
  }

  @Override
  public int hashCode() {
    return super.hashCode();
  }
}
