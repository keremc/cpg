/*
 * Copyright (c) 2019, Fraunhofer AISEC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
package de.fraunhofer.aisec.cpg.graph.types;

import de.fraunhofer.aisec.cpg.frontends.Language;
import de.fraunhofer.aisec.cpg.frontends.LanguageFrontend;
import de.fraunhofer.aisec.cpg.graph.HasType;
import de.fraunhofer.aisec.cpg.graph.declarations.RecordDeclaration;
import de.fraunhofer.aisec.cpg.graph.edge.Properties;
import de.fraunhofer.aisec.cpg.graph.edge.PropertyEdge;
import java.util.*;
import org.jetbrains.annotations.NotNull;
import org.neo4j.ogm.annotation.Relationship;

/**
 * This is the main type in the Type system. ObjectTypes describe objects, as instances of a class.
 * This also includes primitive data types.
 */
public class ObjectType extends Type implements HasType.SecondaryTypeEdge {

  @Override
  public void updateType(@NotNull Collection<? extends Type> typeState) {
    if (this.generics == null) {
      return;
    }
    for (Type t : this.getGenerics()) {
      for (Type t2 : typeState) {
        if (t2.equals(t)) {
          this.replaceGenerics(t, t2);
        }
      }
    }
  }

  public void replaceGenerics(Type oldType, Type newType) {
    if (this.generics == null) {
      return;
    }
    for (int i = 0; i < this.generics.size(); i++) {
      PropertyEdge<Type> propertyEdge = this.generics.get(i);
      if (propertyEdge.getEnd().equals(oldType)) {
        propertyEdge.setEnd(newType);
      }
    }
  }

  // Reference from the ObjectType to its class (RecordDeclaration) only if the class is available
  private RecordDeclaration recordDeclaration = null;

  @Relationship(value = "GENERICS", direction = Relationship.Direction.OUTGOING)
  private List<PropertyEdge<Type>> generics;

  public ObjectType(
      CharSequence typeName,
      List<Type> generics,
      boolean primitive,
      Language<? extends LanguageFrontend> language) {
    super(typeName, language);
    this.generics = PropertyEdge.transformIntoOutgoingPropertyEdgeList(generics, this);
    this.primitive = primitive;
    this.setLanguage(language);
  }

  public ObjectType(
      Type type,
      List<Type> generics,
      boolean primitive,
      Language<? extends LanguageFrontend> language) {
    super(type);
    this.setLanguage(language);
    this.generics = PropertyEdge.transformIntoOutgoingPropertyEdgeList(generics, this);
    this.primitive = primitive;
  }

  /** Empty default constructor for use in Neo4J persistence. */
  public ObjectType() {
    super();
    this.generics = new ArrayList<>();
    this.primitive = false;
  }

  public List<Type> getGenerics() {
    List<Type> genericValues = new ArrayList<>();
    for (PropertyEdge<Type> edge : this.generics) {
      genericValues.add(edge.getEnd());
    }
    return Collections.unmodifiableList(genericValues);
  }

  public List<PropertyEdge<Type>> getGenericPropertyEdges() {
    return this.generics;
  }

  public RecordDeclaration getRecordDeclaration() {
    return recordDeclaration;
  }

  public void setRecordDeclaration(RecordDeclaration recordDeclaration) {
    this.recordDeclaration = recordDeclaration;
  }

  /**
   * @return PointerType to a ObjectType, e.g. int*
   */
  @Override
  public PointerType reference(PointerType.PointerOrigin pointerOrigin) {
    return new PointerType(this, pointerOrigin);
  }

  public PointerType reference() {
    return new PointerType(this, PointerType.PointerOrigin.POINTER);
  }

  /**
   * @return UnknownType, as we cannot infer any type information when dereferencing an ObjectType,
   *     as it is just some memory and its interpretation is unknown
   */
  @Override
  public Type dereference() {
    return UnknownType.getUnknownType(getLanguage());
  }

  @Override
  public Type duplicate() {
    ObjectType newObject =
        new ObjectType(this, this.getGenerics(), this.primitive, this.getLanguage());
    return newObject;
  }

  public void setGenerics(List<Type> generics) {
    this.generics = PropertyEdge.transformIntoOutgoingPropertyEdgeList(generics, this);
  }

  public void addGeneric(Type generic) {
    var propertyEdge = new PropertyEdge<>(this, generic);
    propertyEdge.addProperty(Properties.INDEX, this.generics.size());
    this.generics.add(propertyEdge);
  }

  public void addGenerics(List<Type> generics) {
    for (Type generic : generics) {
      addGeneric(generic);
    }
  }

  @Override
  public boolean isSimilar(Type t) {
    return t instanceof ObjectType that
        && this.getGenerics().equals(that.getGenerics())
        && super.isSimilar(t);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ObjectType that)) return false;
    if (!super.equals(o)) return false;
    return Objects.equals(this.getGenerics(), that.getGenerics())
        && PropertyEdge.propertyEqualsList(generics, that.generics)
        && this.primitive == that.primitive;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), generics, primitive);
  }
}
